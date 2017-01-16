package io.github.lonamiwebs.stringlate.classes.repos;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
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
import io.github.lonamiwebs.stringlate.classes.resources.Resources;
import io.github.lonamiwebs.stringlate.classes.resources.ResourcesParser;
import io.github.lonamiwebs.stringlate.classes.resources.tags.ResTag;
import io.github.lonamiwebs.stringlate.git.GitCloneProgressCallback;
import io.github.lonamiwebs.stringlate.git.GitWrapper;
import io.github.lonamiwebs.stringlate.interfaces.ProgressUpdateCallback;
import io.github.lonamiwebs.stringlate.settings.RepoSettings;

// Class used to inter-operate with locally saved GitHub "repositories"
// What is stored are simply the strings.xml file under a tree directory structure:
/*
.
└── git_url_hash
    ├── default
    │   ├── arrays.xml
    │   └── strings.xml
    ├── es
    │   ├── arrays.xml
    │   └── strings.xml
    ├── …
    │   └── …
    └── info.json
* */
public class RepoHandler implements Comparable<RepoHandler> {

    //region Members

    private final Context mContext;
    private final RepoSettings mSettings;

    private final File mRoot;

    private final Pattern mValuesLocalePattern; // Match locale from "res/values-(...)/strings.xml"
    private final ArrayList<String> mLocales;

    private static final String BASE_DIR = "repos";
    public static final String DEFAULT_LOCALE = "default";

    private static final Pattern OWNER_REPO = Pattern.compile(
            "(?:https?://github\\.com/|git@github.com:)([\\w-]+)/([\\w-]+)(?:/.*|\\.git)?");

    // Match "dontranslate.xml", "do-not-translate.xml", "donottranslate.xml" and such
    private static final Pattern DO_NOT_TRANSLATE = Pattern.compile(
            "(?:do?[ _-]*no?t?|[u|i]n)[ _-]*trans(?:lat(?:e|able))?");

    //endregion

    //region Interfaces and events

    public interface ChangeListener {
        // Called when a repository is either added or removed
        void onRepositoryCountChanged();
    }

    private final static ArrayList<ChangeListener> mChangeListeners = new ArrayList<>();

    public static void addChangeListener(ChangeListener listener) {
        mChangeListeners.add(listener);
    }

    public static void removeChangeListener(ChangeListener listener) {
        mChangeListeners.remove(listener);
    }

    private static void notifyRepositoryCountChanged() {
        for (ChangeListener listener : mChangeListeners)
            listener.onRepositoryCountChanged();
    }

    //endregion

    //region Constructors

    public static RepoHandler fromBundle(Context context, Bundle bundle) {
        return new RepoHandler(context, bundle.getString("giturl"));
    }

    public RepoHandler(Context context, String owner, String repository) {
        this(context, GitWrapper.buildGitHubUrl(owner, repository));
    }

    public RepoHandler(Context context, String gitUrl) {
        mContext = context;
        gitUrl = GitWrapper.getGitUri(gitUrl);

        mRoot = new File(mContext.getFilesDir(), BASE_DIR+"/"+getId(gitUrl));
        mSettings = new RepoSettings(mRoot);
        mSettings.setGitUrl(gitUrl);

        mValuesLocalePattern = Pattern.compile("res/values(?:-([\\w-]+))?/.+?\\.xml");

        mLocales = new ArrayList<>();
        loadLocales();
    }

    private RepoHandler(Context context, File root) {
        mContext = context;
        mRoot = root;
        mSettings = new RepoSettings(mRoot);

        mValuesLocalePattern = Pattern.compile(
                "res/values(?:-([\\w-]+))?/strings\\.xml");

        mLocales = new ArrayList<>();
        loadLocales();
    }

    //endregion

    //region Utilities

    // Retrieves the File object for the given locale
    private File getResourcesFile(@NonNull String locale) {
        return new File(mRoot, locale+"/strings.xml");
    }

    private File getDefaultResourcesFile(@NonNull String filename) {
        return new File(mRoot, DEFAULT_LOCALE+"/"+filename);
    }

    // Used when cloning. An application might have multiple files with the same name
    // but under different directories (i.e., F-Droid has two "strings.xml" files).
    // If this is the case, append an index (i.e., "strings.xml" -> "strings2.xml")
    private File getUniqueDefaultResourcesFile(@NonNull String filename) {
        File result = getDefaultResourcesFile(filename);
        if (result.isFile()) {
            // We assume it contains .xml
            int i = filename.lastIndexOf('.');
            String name = filename.substring(0, i);
            String ext = filename.substring(i);

            i = 2;
            while (result.isFile()) {
                result = getDefaultResourcesFile(name+i+ext);
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
    public boolean isEmpty() { return mLocales.isEmpty(); }

    // Deletes the repository erasing its existence from Earth. Forever. (Unless added again)
    public boolean delete() {
        boolean ok = GitWrapper.deleteRepo(mRoot);
        notifyRepositoryCountChanged();
        return ok;
    }

    private File getTempCloneDir() {
        return new File(mContext.getCacheDir(), "tmp_clone");
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

    public void syncResources(final GitCloneProgressCallback callback, final boolean keepChanges) {
        cloneRepository(callback, keepChanges);
    }

    // Step 1: Temporary clone the GitHub repository
    private void cloneRepository(final GitCloneProgressCallback callback, final boolean keepChanges) {
        callback.onProgressUpdate(
                mContext.getString(R.string.cloning_repo),
                mContext.getString(R.string.cloning_repo_progress, 0.0f));

        new AsyncTask<Void, Void, File>() {
            @Override
            protected File doInBackground(Void... params) {
                File dir = getTempCloneDir();
                GitWrapper.deleteRepo(dir); // Don't care, it's temp and it can't exist on cloning
                if (GitWrapper.cloneRepo(mSettings.getGitUrl(), getTempCloneDir(), callback))
                    return dir;
                else
                    return null;
            }

            @Override
            protected void onPostExecute(File clonedDir) {
                if (clonedDir == null) {
                    callback.onProgressFinished(mContext.getString(R.string.invalid_repo), false);
                    delete(); // Need to delete the settings
                }
                else
                    scanResources(clonedDir, keepChanges, callback);
            }
        }.execute();
    }

    private void scanResources(final File clonedDir, final boolean keepChanges, final ProgressUpdateCallback callback) {
        callback.onProgressUpdate(
                mContext.getString(R.string.scanning_repository),
                mContext.getString(R.string.scanning_repository_long));

        new AsyncTask<Void, Void, ArrayList<File>>() {
            @Override
            protected ArrayList<File> doInBackground(Void... params) {
                return GitWrapper.searchAndroidResources(clonedDir);
            }

            @Override
            protected void onPostExecute(ArrayList<File> foundResources) {
                if (foundResources.size() == 0) {
                    GitWrapper.deleteRepo(clonedDir); // Clean resources
                    delete(); // Need to delete the settings
                    callback.onProgressFinished(
                            mContext.getString(R.string.no_strings_found), false);
                } else {
                    copyResources(clonedDir, foundResources, keepChanges, callback);
                }
            }
        }.execute();
    }

    private void copyResources(final File clonedDir, final ArrayList<File> foundResources,
                               final boolean keepChanges, final ProgressUpdateCallback callback) {
        callback.onProgressUpdate(
                mContext.getString(R.string.copying_res),
                mContext.getString(R.string.copying_res_long));

        // Delete all the previous default resources since their
        // names might have changed, been removed, or some new added.
        mSettings.clearRemotePaths();
        for (File f : getDefaultResourcesFiles())
            f.delete();

        // Iterate over all the found resources
        for (File clonedFile : foundResources) {
            Matcher m = mValuesLocalePattern.matcher(clonedFile.getAbsolutePath());

            // Ensure that we can tell the locale from the path (otherwise it's invalid)
            if (m.find()) {

                // Determine the locale, and the final output file (we may have changes there)
                File outputFile;
                if (m.group(1) == null) {
                    // If this is the default locale, save its remote path
                    // and clean it by removing all the translatable="false" tags
                    //
                    // However, if the file name is something like "do not translate", skip it
                    Matcher untranslatable = DO_NOT_TRANSLATE.matcher(clonedFile.getName());
                    if (untranslatable.find())
                        continue;

                    // First load the cloned resources to ensure it contains translatable strings
                    Resources clonedResources = Resources.fromFile(clonedFile);
                    if (!clonedResources.isEmpty()) {
                        // Clean the untranslatable strings while saving the clean file
                        outputFile = getUniqueDefaultResourcesFile(clonedFile.getName());
                        ResourcesParser.cleanXml(clonedFile, outputFile);

                        // Skip the '/' at the beginning (substring +1)
                        String remotePath = clonedFile.getAbsolutePath()
                                .substring(clonedDir.getAbsolutePath().length()+1);

                        addRemotePath(outputFile.getName(), remotePath);
                    }
                } else {
                    // Get the file corresponding to this locale (group(1))
                    outputFile = getResourcesFile(m.group(1));

                    // Load in memory the old saved resources. We need to work
                    // on this file because we're going to be merging changes
                    // in order to handle multiple cloned resource files

                    // TODO Optimize to pack strings corresponding to the same locale and
                    // process it all at once, thus avoiding loading oldResources all the time
                    Resources oldResources = Resources.fromFile(outputFile);

                    // Also load the new resources (to both ensure it's OK and to copy the strings)
                    Resources newResources = Resources.fromFile(clonedFile);

                    if (keepChanges) {
                        // We want to our previous keep changes, and so we
                        // will only add the tags which were NOT modified
                        for (ResTag rt : newResources) {
                            if (!oldResources.wasModified(rt.getId())) {
                                oldResources.addTag(rt);
                            }
                        }
                    } else {
                        // We don't want to keep any change, simply merge the tags
                        for (ResTag rt : newResources) {
                            oldResources.addTag(rt);
                        }
                    }

                    // Save the changes
                    oldResources.save();
                }
            }
        }

        // Check out if we have any icon for this repository
        File icon = GitWrapper.findProperIcon(clonedDir, mContext);
        if (icon != null) {
            // We have an icon to show, copy it to our repository root
            // and save it's path (we must keep track of the used extension)
            File newIcon = new File(mRoot, icon.getName());
            if (!newIcon.isFile() || newIcon.delete()) {
                if (icon.renameTo(newIcon))
                    mSettings.setIconFile(newIcon);
            }
        }

        GitWrapper.deleteRepo(clonedDir); // Clean resources
        notifyRepositoryCountChanged();
        callback.onProgressFinished(null, true);
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
    public String applyTemplate(File template, String locale) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (applyTemplate(template, locale, out))
            return out.toString();
        else
            return "";
    }

    // TODO Why do I load the resources all the time - can't I just pass the loaded one?
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
                } catch (IOException ignored) { }
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

    public String getLastLocale() {
        return mSettings.getLastLocale();
    }

    public void setLastLocale(String locale) {
        mSettings.setLastLocale(locale);
    }

    public boolean isGitHubRepository() {
        return OWNER_REPO.matcher(mSettings.getGitUrl()).matches();
    }

    private void addRemotePath(String filename, String remotePath) {
        mSettings.addRemotePath(filename, remotePath);
    }

    public boolean hasRemoteUrls() {
        return getDefaultResourcesFiles().length == mSettings.getRemotePaths().size();
    }

    // Return a map consisting of (default local resources/templates, remote path)
    // and replacing the "values" by the corresponding "values-xx"
    public HashMap<File, String> getTemplateRemotePaths(String locale) {
        HashMap<File, String> result = new HashMap<>();
        HashMap<String, String> fileRemote =  mSettings.getRemotePaths();
        for (Map.Entry<String, String> fr : fileRemote.entrySet()) {
            File template = getDefaultResourcesFile(fr.getKey());
            String remote = fr.getValue().replace("/values/", "/values-"+locale+"/");
            result.put(template, remote);
        }
        return result;
    }

    public File getIconFile() {
        return mSettings.getIconFile();
    }

    //endregion

    //region To other objects

    @Override
    public String toString() {
        // https:// part is a bit redundant, also omit the `.git` part
        String url = mSettings.getGitUrl();
        try {
            int end = url.endsWith(".git") ? url.lastIndexOf('.') : url.length();
            return url.substring(url.indexOf("://") + 3, end);
        } catch (StringIndexOutOfBoundsException e) {
            Log.w("RepoHandler", "Please report that \""+url+"\" got somehow saved…");
            return url; // We must have a really weird url. Maybe saved invalid repo somehow?
        }
    }

    public String getHost() {
        String url = mSettings.getGitUrl();
        try {
            return new URL(url).getHost();
        } catch (MalformedURLException e) {
            // Should never happen
            e.printStackTrace();
        }
        return url;
    }

    public String getPath() {
        String url = mSettings.getGitUrl();
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

    public String toString(boolean onlyRepo) {
        String result = toString();
        if (onlyRepo)
            return result.substring(result.lastIndexOf('/')+1);
        else
            return result;
    }

    public String toOwnerRepo() throws InvalidObjectException {
        Matcher m = OWNER_REPO.matcher(mSettings.getGitUrl());
        if (m.matches())
            return String.format("%s/%s", m.group(1), m.group(2));
        else
            throw new InvalidObjectException(
                    "Only repositories with a GitHub url can be converted to owner and repository.");
    }

    public Bundle toBundle() {
        Bundle result = new Bundle();
        result.putString("giturl", mSettings.getGitUrl());
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

    // TODO As usual, remove this code by version 1.0 or so
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
                        owner.getName(), repository.getName());

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
}
