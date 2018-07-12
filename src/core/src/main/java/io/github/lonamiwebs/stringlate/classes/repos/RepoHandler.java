package io.github.lonamiwebs.stringlate.classes.repos;

import net.gsantner.opoc.util.FileUtils;
import net.gsantner.opoc.util.ZipUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import io.github.lonamiwebs.stringlate.classes.Messenger;
import io.github.lonamiwebs.stringlate.classes.git.GitHub;
import io.github.lonamiwebs.stringlate.classes.locales.LocaleString;
import io.github.lonamiwebs.stringlate.classes.resources.Resources;
import io.github.lonamiwebs.stringlate.classes.resources.ResourcesParser;
import io.github.lonamiwebs.stringlate.classes.resources.tags.ResTag;
import io.github.lonamiwebs.stringlate.classes.sources.SourceSettings;
import io.github.lonamiwebs.stringlate.interfaces.StringsSource;

// Represents a locally saved string repository, which can be synchronized from any StringsSource
public class RepoHandler implements Comparable<RepoHandler> {

    //region Members

    public final RepoSettings settings; // Public to avoid a lot of wrapper methods
    private final SourceSettings mSourceSettings;

    public final File mRoot, mCacheDir;
    private final File mProgressFile;

    private final ArrayList<String> mLocales = new ArrayList<>();

    public static final String DEFAULT_LOCALE = "default";

    private final static ReentrantLock syncingLock = new ReentrantLock();
    private final static HashSet<File> rootsInSync = new HashSet<>();
    private StringsSource mSyncingSource;
    private boolean wasCancelled;

    //endregion

    //region Constructors

    public RepoHandler(final String source, final File workDir, final File cacheDir) {
        mRoot = new File(workDir, getId(source));
        mCacheDir = cacheDir;
        settings = new RepoSettings(mRoot);
        settings.setSource(source);

        mSourceSettings = new SourceSettings(mRoot);
        settings.checkUpgradeSettingsToSpecific(mSourceSettings);
        mProgressFile = new File(mRoot, "translation_progress.json");

        loadLocales();
    }

    public RepoHandler(final File root, final File cacheDir) {
        mRoot = root;
        mCacheDir = cacheDir;
        settings = new RepoSettings(mRoot);
        mSourceSettings = new SourceSettings(mRoot);
        settings.checkUpgradeSettingsToSpecific(mSourceSettings);

        mProgressFile = new File(mRoot, "translation_progress.json");

        loadLocales();
    }

    //endregion

    //region Utilities

    // Retrieves the File object for the given locale
    private File getResourcesFile(final String locale) {
        if (locale == null)
            throw new IllegalArgumentException("locale cannot be null");
        return new File(mRoot, locale + "/strings.xml");
    }

    private File getDefaultResourcesFile(final String filename) {
        if (filename == null)
            throw new IllegalArgumentException("filename cannot be null");
        return new File(mRoot, DEFAULT_LOCALE + "/" + filename);
    }

    // An application may want to split their translations into several files. We
    // can just save them as strings.xml, strings2.xml, strings3.xml... and save
    // a map somewhere else to store "strings%d.xml -> original-path.xml".
    private File getUniqueDefaultResourcesFile() {
        File result = getDefaultResourcesFile("strings.xml");
        if (result.isFile()) {
            int i = 2;
            while (result.isFile()) {
                result = getDefaultResourcesFile("strings" + i + ".xml");
                i++;
            }
        }
        return result;
    }

    // Never returns null
    public File[] getDefaultResourcesFiles() {
        File root = new File(mRoot, DEFAULT_LOCALE);
        if (root.isDirectory()) {
            File[] files = root.listFiles();
            if (files != null)
                return files;
        }
        return new File[0];
    }

    public boolean hasDefaultLocale() {
        return getDefaultResourcesFiles().length > 0;
    }

    // Determines whether a given locale is saved or not
    private boolean hasLocale(final String locale) {
        return getResourcesFile(locale).isFile();
    }

    // Determines whether any file has been modified,
    // i.e. it is not the original downloaded file any more.
    // Note that previous modifications do NOT imply the file being unsaved.
    public boolean anyModified() {
        for (String locale : mLocales)
            if (Resources.fromFile(getResourcesFile(locale)).wasModified())
                return true;
        return false;
    }

    // Determines whether the repository is empty (has no saved locales) or not
    public boolean isEmpty() {
        return mLocales.isEmpty();
    }

    // Deletes the repository erasing its existence from Earth. Forever. (Unless added again)
    public boolean delete() {
        boolean ok = FileUtils.deleteRecursive(mRoot);
        Messenger.notifyRepoRemoved(this);
        return ok;
    }

    private File getTempImportDir() {
        return new File(mCacheDir, "tmp_import");
    }

    private File getTempImportBackupDir() {
        return new File(mCacheDir, "tmp_import_backup");
    }

    private static String getId(String gitUrl) {
        return Integer.toHexString(gitUrl.hashCode());
    }

    //endregion

    //region Locales

    //region Loading locale files

    private void loadLocales() {
        mLocales.clear();
        if (mRoot.isDirectory()) {
            for (File localeDir : mRoot.listFiles()) {
                if (localeDir.isDirectory()) {
                    mLocales.add(localeDir.getName());
                }
            }
        }
        Collections.sort(mLocales, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return LocaleString.getDisplay(s1).compareTo(LocaleString.getDisplay(s2));
            }
        });
    }

    public ArrayList<String> getLocales() {
        return mLocales;
    }

    //endregion

    //region Creating and deleting locale files

    public boolean createLocale(String locale) {
        if (hasLocale(locale))
            return true;

        Resources resources = Resources.fromFile(getResourcesFile(locale));
        if (!resources.save())
            return false;

        mLocales.add(locale);
        return true;
    }

    public void deleteLocale(String locale) {
        if (hasLocale(locale)) {
            Resources.fromFile(getResourcesFile(locale)).delete();
            mLocales.remove(locale);
        }
    }

    //endregion

    //region Downloading locale files

    public boolean isSyncing() {
        syncingLock.lock();
        final boolean isSyncing = rootsInSync.contains(mRoot);
        syncingLock.unlock();
        return isSyncing;
    }

    public boolean syncResources(final StringsSource source,
                                 final int desiredIconDpi,
                                 final Messenger.OnSyncProgress callback) {

        syncingLock.lock();
        if (rootsInSync.contains(mRoot)) {
            syncingLock.unlock();
            return false; // Already syncing, this won't sync
        } else {
            rootsInSync.add(mRoot);
            mSyncingSource = source;
            wasCancelled = false;
            syncingLock.unlock();
        }

        try {
            return doSyncResources(source, desiredIconDpi, callback);
        } finally {
            syncingLock.lock();
            rootsInSync.remove(mRoot);
            mSyncingSource = null;
            syncingLock.unlock();
            source.dispose();
        }
    }

    public void cancel() {
        if (mSyncingSource != null) {
            mSyncingSource.cancel();
            wasCancelled = true;
        }
    }

    public boolean wasCancelled() {
        return wasCancelled;
    }

    // Should be called from a background thread
    private boolean doSyncResources(final StringsSource source,
                                    final int desiredIconDpi,
                                    final Messenger.OnSyncProgress callback) {

        if (!mSourceSettings.getName().equals(source.getName())) {
            // if (!sourceName.isEmpty()) { ... }
            // TODO Warn the user they're using a different source for the strings?
            mSourceSettings.reset(source.getName());
        }

        final File tmpWorkDir = new File(mCacheDir, "tmp_sync_" + mRoot.getName());
        if (tmpWorkDir.isDirectory())
            if (!FileUtils.deleteRecursive(tmpWorkDir))
                return false;

        if (!source.setup(mSourceSettings, tmpWorkDir, desiredIconDpi, callback))
            return false;

        callback.onUpdate(2, (0f / 3f));

        // Delete all the previous default resources since their
        // names might have changed, been removed, or some new added.
        settings.clearRemotePaths();
        for (File f : getDefaultResourcesFiles())
            if (!f.delete())
                return false;

        for (String locale : source.getLocales()) {
            if (locale == null)
                continue; // Should not happen

            // Load in memory the old saved resources. We need to work
            // on this file because we're going to be merging changes.
            Resources resources = loadResources(locale);

            // Add new translated tags without overwriting existing ones
            for (ResTag rt : source.getResources(locale))
                if (!resources.wasModified(rt.getId()))
                    resources.addTag(rt);

            // Save the changes
            resources.save();
        }

        callback.onUpdate(2, (1f / 3f));

        // Default resources are treated specially, since their name matters. The name for
        // non-default resources doesn't because it can be inferred from defaults' (for now).
        for (String originalName : source.getDefaultResources()) {
            boolean okay;
            final File resourceFile = getUniqueDefaultResourcesFile();

            final String xml = source.getDefaultResourceXml(originalName);
            if (xml == null) {
                // We don't know how the original XML looked like, that's okay
                final Resources resources = Resources.fromFile(resourceFile);
                for (ResTag rt : source.getDefaultResource(originalName))
                    resources.addTag(rt); // Copy the resources to the new local file

                okay = resources.save();
            } else {
                // We have the original XML available, so clean it up and preserve its structure
                okay = ResourcesParser.cleanXml(xml, resourceFile);
            }

            if (okay) {
                // Save the map unique -> original since this is a valid file
                settings.addRemotePath(resourceFile.getName(), originalName);
            } else {
                // Something went wrong, either saving, cleaning the XML, or it has no strings
                // Clean up the file we may have made, if it exists, or give up if it fails
                if (resourceFile.isFile())
                    if (!resourceFile.delete())
                        return false;
            }
        }

        callback.onUpdate(2, (2f / 3f));

        // Check out if we have any icon for this repository
        File icon = source.getIcon();
        if (icon != null) {
            // We have an icon to show, copyFile it to our repository root
            // and save it's path (we must keep track of the used extension)
            File newIcon = new File(mRoot, icon.getName());
            if (!newIcon.isFile() || newIcon.delete()) {
                if (FileUtils.copyFile(icon, newIcon))
                    settings.setIconFile(newIcon);
            }
        }

        // Clean old unused strings which now don't exist on the default resources files
        unusedStringsCleanup();
        loadLocales(); // Reload the locales

        callback.onUpdate(2, (3f / 3f));

        return true;
    }

    private void unusedStringsCleanup() {
        final Resources defaultResources = loadDefaultResources();

        for (String locale : getLocales()) {
            if (locale.equals(DEFAULT_LOCALE))
                continue;

            final Resources resources = loadResources(locale);

            // Find those which we need to remove (we can't remove them right
            // away unless with used an Iterator<ResTag>, but this also works)
            final ArrayList<String> toRemove = new ArrayList<>();
            for (ResTag rt : resources)
                if (!defaultResources.contains(rt.getId()))
                    toRemove.add(rt.getId());

            // Do remove the unused strings and save
            for (String remove : toRemove)
                resources.deleteId(remove);

            resources.save();
        }
    }

    //endregion

    //region Loading resources

    public Resources loadDefaultResources() {
        // Mix up all the resource files into one
        Resources resources = Resources.empty();
        for (File f : getDefaultResourcesFiles()) {
            for (ResTag rt : Resources.fromFile(f)) {
                resources.addTag(rt);
            }
        }
        return resources;
    }

    public Resources loadResources(final String locale) {
        return Resources.fromFile(getResourcesFile(locale));
    }

    // Returns "" if the template wasn't applied successfully (never null)
    // TODO Handle the above case more gracefully, display a toast error maybe
    public String applyTemplate(final File template, final String locale) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (applyTemplate(template, locale, out))
            return out.toString();
        else
            return "";
    }

    // Returns TRUE if the template can be applied.
    // This means that if for the given template there is no available
    // string on the given locale, the apply cannot be applied since
    // there will be no strings to replace.
    public boolean canApplyTemplate(File template, String locale) {
        if (hasLocale(locale) && template.isFile()) {
            Resources templateResources = Resources.fromFile(template);
            Resources localeResources = loadResources(locale);
            for (ResTag rt : localeResources)
                if (templateResources.contains(rt.getId()))
                    return true;
        }
        return false;
    }

    // TODO Why do I load the resources all the time - can't I just pass the loaded one?
    // Returns TRUE if the template was applied successfully
    public boolean applyTemplate(final File template, final String locale, final OutputStream out) {
        return hasLocale(locale) &&
                template.isFile() &&
                ResourcesParser.applyTemplate(template, loadResources(locale), out);
    }

    // Never returns null
    public String mergeDefaultTemplate(final String locale) {
        return mergeDefaultTemplate(locale,
                "<!-- File \"", "\" -->\n", "\n");
    }

    public String mergeDefaultTemplate(
            final String locale,
            final String beforeName, final String betweenNameXml, final String afterXml) {
        // TODO What should we do if any fails? How can it even fail? No translations for a file?
        File[] files = getDefaultResourcesFiles();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HashMap<String, String> paths = settings.getRemotePaths();
        for (File template : files) {
            String path = paths.get(template.getName());
            try {
                out.write(beforeName.getBytes());
                out.write((path == null ? template.getName() : path).getBytes());
                out.write(betweenNameXml.getBytes());
                applyTemplate(template, locale, out);
                out.write(afterXml.getBytes());
            } catch (IOException ignored) {
            }
        }
        return out.toString();
    }

    //endregion

    //endregion

    //region Static repository listing

    public static ArrayList<RepoHandler> listRepositories(final File workDir, final File cacheDir) {
        ArrayList<RepoHandler> repositories = new ArrayList<>();

        if (workDir.isDirectory()) {
            for (File f : workDir.listFiles()) {
                if (isValidRepoDir(f)) {
                    repositories.add(new RepoHandler(f, cacheDir));
                }
            }
        }

        return repositories;
    }

    private static boolean isValidRepoDir(File dir) {
        return dir.isDirectory() && RepoSettings.exists(dir);
    }

    //endregion

    //region Settings

    // Never returns null
    public String getSourceName() {
        return mSourceSettings.getName();
    }

    public boolean isGitHubRepository() {
        if (!mSourceSettings.getName().equals("git"))
            return false;

        try {
            GitHub.getGitHubOwnerRepo(settings.getSource());
            return true;
        } catch (InvalidObjectException ignored) {
            return false;
        }
    }

    public boolean hasRemoteUrls() {
        return mSourceSettings.getName().equals("git") &&
                getDefaultResourcesFiles().length == settings.getRemotePaths().size();
    }

    // Return a map consisting of (default local resources/templates, remote path)
    // and replacing the "values" by the corresponding "values-xx"
    public HashMap<File, String> getTemplateRemotePaths(String locale) {
        HashMap<File, String> result = new HashMap<>();
        HashMap<String, String> fileRemote = settings.getRemotePaths();
        for (Map.Entry<String, String> fr : fileRemote.entrySet()) {
            File template = getDefaultResourcesFile(fr.getKey());
            String remote = fr.getValue().replace("/values/", "/values-" + locale + "/");
            result.put(template, remote);
        }
        return result;
    }

    // Never returns null
    public String getUsedTranslationService() {
        final String result = (String) mSourceSettings.get("translation_service");
        return result == null ? "" : result;
    }

    // Never returns null
    public ArrayList<String> getRemoteBranches() {
        if (getSourceName().equals("git")) {
            return mSourceSettings.getArray("remote_branches");
        } else {
            return new ArrayList<>();
        }
    }

    // Used to save the translation progress, calculated on the
    // TranslateActivity, to quickly reuse it on the history screen
    public boolean saveProgress(RepoProgress progress) {
        try {
            return FileUtils.writeFile(mProgressFile, progress.toJson().toString());
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Gets the progress of the last calculated progress locale
    public RepoProgress loadProgress() {
        try {
            final String json = FileUtils.readTextFile(mProgressFile);
            if (!json.isEmpty())
                return RepoProgress.fromJson(new JSONObject(json));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    //endregion

    //region To other objects

    @Override
    public String toString() {
        // https:// part is a bit redundant, also omit the `.git` part if it exists
        String url = settings.getSource();
        try {
            int end = url.endsWith(".git") ? url.lastIndexOf('.') : url.length();
            return url.substring(url.indexOf("://") + 3, end);
        } catch (StringIndexOutOfBoundsException e) {
            return url; // We must have a really weird url. Maybe saved invalid repo somehow?
        }
    }

    public String getHost() {
        String url = settings.getSource();
        try {
            return new URL(url).getHost();
        } catch (MalformedURLException e) {
            // Should never happen
            e.printStackTrace();
        }
        return url;
    }

    public String getPath() {
        String url = settings.getSource();
        try {
            String path = new URL(url).getPath();
            int end = path.endsWith(".git") ? path.lastIndexOf('.') : path.length();
            return path.substring(0, end);
        } catch (MalformedURLException e) {
            // Should never happen
            e.printStackTrace();
        }
        return url;
    }

    public String getProjectName() {
        String name = settings.getProjectName();
        if (name.isEmpty()) {
            name = nameFromSource(settings.getSource());
            settings.setProjectName(name);
        }

        return name;
    }

    private static String nameFromSource(String source) {
        int slash = source.lastIndexOf('/');
        if (slash < 0)
            return source; // Should not happen

        source = source.substring(slash + 1);
        int dot = source.lastIndexOf('.');
        if (dot >= 0)
            source = source.substring(0, dot);
        return source;
    }

    public String toOwnerRepo() throws InvalidObjectException {
        return GitHub.getGitHubOwnerRepo(settings.getSource());
    }

    //endregion

    //region Interface implementations

    @Override
    public int compareTo(final RepoHandler o) {
        return toString().compareTo(o.toString());
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj ||
                (obj instanceof RepoHandler && mRoot.equals(((RepoHandler) obj).mRoot));
    }

    //endregion

    //region Importing and exporting

    public void importZip(InputStream inputStream) {
        try {
            // Get temporary import directory and delete any previous directory
            File dir = getTempImportDir();
            File backupDir = getTempImportBackupDir();
            if (!FileUtils.deleteRecursive(dir) || !FileUtils.deleteRecursive(backupDir))
                throw new IOException("Could not delete old temporary directories.");

            // Unzip the given input stream
            ZipUtils.unzip(inputStream, dir, false, null, -1f);

            // Ensure it's a valid repository (only one parent directory, containing settings)
            final File[] unzippedFiles = dir.listFiles();
            if (unzippedFiles == null || unzippedFiles.length != 1)
                throw new IOException("There should only be 1 unzipped file (the repository's root).");

            final File root = unzippedFiles[0];
            if (!RepoSettings.exists(root))
                throw new IOException("The specified .zip file does not seem valid.");

            // Nice, unzipping worked. Now try moving the current repository
            // to yet another temporary location, because we don't want to lose
            // it in case something goes wrong and we need to revert…
            if (!mRoot.renameTo(backupDir))
                throw new IOException("Could not move the current repository to its backup location.");

            if (!root.renameTo(mRoot)) {
                // Try reverting the state, hopefully no data was lost
                String extra = backupDir.renameTo(mRoot) ? "" : " Failed to recover its previous state.";
                throw new IOException("Could not move the temporary repository to its new location." + extra);
            }

            Messenger.notifyRepoAdded(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exportZip(OutputStream output) {
        try {
            ZipUtils.zipFolder(mRoot, output);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //endregion
}
