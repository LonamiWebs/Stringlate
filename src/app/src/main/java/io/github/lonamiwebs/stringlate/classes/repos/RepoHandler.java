package io.github.lonamiwebs.stringlate.classes.repos;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;

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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.LocaleString;
import io.github.lonamiwebs.stringlate.classes.Messenger;
import io.github.lonamiwebs.stringlate.classes.resources.Resources;
import io.github.lonamiwebs.stringlate.classes.resources.ResourcesParser;
import io.github.lonamiwebs.stringlate.classes.resources.tags.ResTag;
import io.github.lonamiwebs.stringlate.classes.sources.GitSource;
import io.github.lonamiwebs.stringlate.git.GitWrapper;
import io.github.lonamiwebs.stringlate.interfaces.ProgressUpdateCallback;
import io.github.lonamiwebs.stringlate.interfaces.StringsSource;
import io.github.lonamiwebs.stringlate.settings.RepoSettings;
import io.github.lonamiwebs.stringlate.settings.SourceSettings;
import io.github.lonamiwebs.stringlate.utilities.Utils;
import io.github.lonamiwebs.stringlate.utilities.ZipUtils;

// Represents a locally saved string repository, which can be synchronized from any StringsSource
public class RepoHandler implements Comparable<RepoHandler> {

    //region Members

    private final Context mContext;
    public final RepoSettings settings; // Public to avoid a lot of wrapper methods
    private final SourceSettings mSourceSettings;

    private final File mRoot;
    private final File mProgressFile;

    private final ArrayList<String> mLocales = new ArrayList<>();

    private static final String BASE_DIR = "repos";
    public static final String DEFAULT_LOCALE = "default";

    //endregion

    //region Constructors

    public static RepoHandler fromBundle(Context context, Bundle bundle) {
        return new RepoHandler(context, bundle.getString("source"));
    }

    public RepoHandler(Context context, final String source) {
        mContext = context;

        mRoot = new File(mContext.getFilesDir(), BASE_DIR + "/" + getId(source));
        settings = new RepoSettings(mRoot);
        settings.setSource(source);

        mSourceSettings = new SourceSettings(mRoot);
        settings.checkUpgradeSettingsToSpecific(mSourceSettings);
        mProgressFile = new File(mRoot, "translation_progress.json");

        loadLocales();
    }

    private RepoHandler(Context context, File root) {
        mContext = context;
        mRoot = root;
        settings = new RepoSettings(mRoot);
        mSourceSettings = new SourceSettings(mRoot);
        settings.checkUpgradeSettingsToSpecific(mSourceSettings);

        mProgressFile = new File(mRoot, "translation_progress.json");

        loadLocales();
    }

    //endregion

    //region Utilities

    // Retrieves the File object for the given locale
    private File getResourcesFile(@NonNull String locale) {
        return new File(mRoot, locale + "/strings.xml");
    }

    private File getDefaultResourcesFile(@NonNull String filename) {
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

    @NonNull
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
    private boolean hasLocale(@NonNull String locale) {
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
        boolean ok = Utils.deleteRecursive(mRoot);
        Messenger.notifyRepoCountChange();
        return ok;
    }

    private File getTempImportDir() {
        return new File(mContext.getCacheDir(), "tmp_import");
    }

    private File getTempImportBackupDir() {
        return new File(mContext.getCacheDir(), "tmp_import_backup");
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

    // Should be called from a background thread
    public boolean syncResources(final StringsSource source,
                                 final ProgressUpdateCallback callback) {

        if (!mSourceSettings.getName().equals(source.getName())) {
            // if (!sourceName.isEmpty()) { ... }
            // TODO Warn the user they're using a different source for the strings?
            mSourceSettings.reset(source.getName());
        }

        if (!source.setup(mContext, mSourceSettings, callback)) {
            source.dispose();
            delete(); // Delete settings
        }

        callback.onProgressUpdate(
                mContext.getString(R.string.copying_res),
                mContext.getString(R.string.copying_res_long)
        );

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

        // Check out if we have any icon for this repository
        File icon = source.getIcon();
        if (icon != null) {
            // We have an icon to show, copy it to our repository root
            // and save it's path (we must keep track of the used extension)
            File newIcon = new File(mRoot, icon.getName());
            if (!newIcon.isFile() || newIcon.delete()) {
                if (icon.renameTo(newIcon)) // TODO Do NOT rename the icon, rather copy it
                    settings.setIconFile(newIcon);
            }
        }

        // Clean old unused strings which now don't exist on the default resources files
        unusedStringsCleanup();

        source.dispose(); // Clean resources
        loadLocales(); // Reload the locales

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

    public Resources loadResources(@NonNull String locale) {
        return Resources.fromFile(getResourcesFile(locale));
    }

    @NonNull
    // Returns "" if the template wasn't applied successfully
    // TODO Handle the above case more gracefully, display a toast error maybe
    public String applyTemplate(File template, String locale) {
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
    public boolean applyTemplate(File template, String locale, OutputStream out) {
        return hasLocale(locale) &&
                template.isFile() &&
                ResourcesParser.applyTemplate(template, loadResources(locale), out);
    }

    @NonNull
    public String mergeDefaultTemplate(String locale) {
        // TODO What should we do if any fails? How can it even fail? No translations for a file?
        File[] files = getDefaultResourcesFiles();
        if (files.length > 1) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (File template : files) {
                String header = mContext.getString(R.string.xml_comment_filename, template.getName());
                try {
                    out.write(header.getBytes());
                    applyTemplate(template, locale, out);
                    out.write("\n".getBytes());
                } catch (IOException ignored) {
                }
            }
            return out.toString();
        } else {
            return applyTemplate(files[0], locale);
        }
    }

    //endregion

    //endregion

    //region Static repository listing

    public static ArrayList<RepoHandler> listRepositories(Context context) {
        ArrayList<RepoHandler> repositories = new ArrayList<>();

        File root = new File(context.getFilesDir(), BASE_DIR);
        if (root.isDirectory()) {
            for (File f : root.listFiles()) {
                if (isValidRepoDir(f)) {
                    repositories.add(new RepoHandler(context, f));
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

    @NonNull
    public String getSourceName() {
        return mSourceSettings.getName();
    }

    public boolean isGitHubRepository() {
        if (!mSourceSettings.getName().equals("git"))
            return false;

        try {
            GitWrapper.getGitHubOwnerRepo(settings.getSource());
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

    @NonNull
    public String getUsedTranslationService() {
        final String result = (String)mSourceSettings.get("translation_service");
        return result == null ? "" : result;
    }

    @NonNull
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
            return Utils.writeFile(mProgressFile, progress.toJson().toString());
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Gets the progress of the last calculated progress locale
    public RepoProgress loadProgress() {
        try {
            final String json = Utils.readFile(mProgressFile);
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
            Log.w("RepoHandler", "Please report that \"" + url + "\" got somehow saved…");
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
        Pair<String, String> pair = GitWrapper.getGitHubOwnerRepo(settings.getSource());
        return String.format("%s/%s", pair.first, pair.second);
    }

    public Bundle toBundle() {
        Bundle result = new Bundle();
        result.putString("source", settings.getSource());
        return result;
    }

    //endregion

    //region Interface implementations

    @Override
    public int compareTo(@NonNull RepoHandler o) {
        return toString().compareTo(o.toString());
    }

    //endregion

    //region Backwards-compatible code

    public static boolean checkUpgradeRepositories(Context context) {
        // The regex used to match for 'strings-locale.xml' files
        Pattern localesPattern = Pattern.compile("strings(?:-([\\w-]+))?\\.xml");

        File root = new File(context.getFilesDir(), BASE_DIR);
        if (!root.isDirectory()) {
            // The user never used Stringlate
            return true;
        }

        boolean allOk = true;
        for (File owner : root.listFiles()) {
            if (isValidRepoDir(owner)) {
                // Skip repositories which are valid - these don't need to be converted
                continue;
            }

            for (File repository : owner.listFiles()) {
                if (!repository.isDirectory()) {
                    // If this is not a directory, whatever it is, is not valid
                    continue;
                }

                // We now have a valid old repository.
                // The first step is to create a new instance so the settings get created
                RepoHandler repo = new RepoHandler(context,
                        GitWrapper.buildGitHubUrl(owner.getName(), repository.getName()));

                // The second step is to scan the files under 'repository'
                // These will be named 'strings-locale.xml'
                for (File strings : repository.listFiles()) {
                    Matcher m = localesPattern.matcher(strings.getName());
                    if (m.matches()) {
                        // Retrieve the locale and copy it to the new location
                        if (m.group(1) == null) {
                            // Default locale. Make sure to clean it too. Since only
                            // strings.xml files were supported, assume this is the name
                            File newStrings = repo.getDefaultResourcesFile("strings.xml");
                            ResourcesParser.cleanXml(strings, newStrings);
                        } else {
                            String locale = m.group(1);
                            File newStrings = repo.getResourcesFile(locale);

                            // Ensure the parent directory exists before moving the file
                            if (!newStrings.getParentFile().isDirectory())
                                allOk &= newStrings.getParentFile().mkdirs();

                            allOk &= strings.renameTo(newStrings);
                        }
                    }
                    if (strings.isFile()) {
                        // After we're done processing the file, delete it
                        allOk &= strings.delete();
                    }
                }
                // After we're done processing the whole repository, delete it
                allOk &= repository.delete();
            }
            // After we're done processing all the repositories from this owner, delete it
            allOk &= owner.delete();
        }
        return allOk;
    }

    //endregion

    //region Importing and exporting

    public void importZip(InputStream inputStream) {
        try {
            // Get temporary import directory and delete any previous directory
            File dir = getTempImportDir();
            File backupDir = getTempImportBackupDir();
            if (!Utils.deleteRecursive(dir) || !Utils.deleteRecursive(backupDir))
                throw new IOException("Could not delete old temporary directories.");

            // Unzip the given input stream
            ZipUtils.unzipRecursive(inputStream, dir);

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

            // Re-notify that the imported repository succeeded
            Messenger.notifyRepoCountChange();
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
