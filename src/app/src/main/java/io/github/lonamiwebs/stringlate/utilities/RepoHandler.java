package io.github.lonamiwebs.stringlate.utilities;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InvalidObjectException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.LocaleString;
import io.github.lonamiwebs.stringlate.classes.resources.Resources;
import io.github.lonamiwebs.stringlate.classes.resources.ResourcesParser;
import io.github.lonamiwebs.stringlate.classes.resources.ResourcesString;
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

    public boolean hasDefaultLocale() {
        return hasLocale(DEFAULT_LOCALE);
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

        // TODO Set the remote path… somehow
        Resources resources = Resources.fromFile(getResourcesFile(locale));
        if (resources == null || !resources.save())
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

    public void syncResources(ProgressUpdateCallback callback, boolean keepChanges) {
        cloneRepository(callback, keepChanges);
    }

    // Step 1: Temporary clone the GitHub repository
    private void cloneRepository(final ProgressUpdateCallback callback, final boolean keepChanges) {
        callback.onProgressUpdate(
                mContext.getString(R.string.cloning_repo),
                mContext.getString(R.string.cloning_repo_long));

        new AsyncTask<Void, Void, File>() {
            @Override
            protected File doInBackground(Void... params) {
                File dir = getTempCloneDir();
                GitWrapper.deleteRepo(dir); // Don't care, it's temp and it can't exist on cloning
                if (GitWrapper.cloneRepo(mSettings.getGitUrl(), getTempCloneDir()))
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

        // Iterate over all the found resources
        for (File clonedFile : foundResources) {
            Matcher m = mValuesLocalePattern.matcher(clonedFile.getAbsolutePath());

            // Ensure that we can tell the locale from the path (otherwise it's invalid)
            if (m.find()) {

                // Determine the locale, and the final output file (we may have changes there)
                String locale = m.group(1) == null ? DEFAULT_LOCALE : m.group(1);
                File outputFile = getResourcesFile(locale);

                // Load in memory the new cloned file, to also ensure it's OK
                Resources clonedResources = Resources.fromFile(clonedFile, clonedDir);
                if (clonedResources != null) {
                    // It's a valid file
                    if (keepChanges) {
                        // Overwrite our changes, if any
                        Resources oldResources = Resources.fromFile(outputFile);
                        if (oldResources != null) {
                            for (ResourcesString rs : oldResources) {
                                if (rs.wasModified())
                                    clonedResources.setContent(rs.getId(), rs.getContent());
                            }
                        }
                    }
                    // Save to keep our remote url
                    clonedResources.forceSave();

                    // Now that we're done, move the file to its destination
                    if (outputFile.getParentFile().isDirectory()) {
                        if (outputFile.isFile())
                            outputFile.delete();
                    } else {
                        outputFile.getParentFile().mkdirs();
                    }
                    clonedFile.renameTo(outputFile);
                }
            }
        }

        GitWrapper.deleteRepo(clonedDir); // Clean resources
        notifyRepositoryCountChanged();
        callback.onProgressFinished(null, true);
    }

    //endregion

    //region Loading resources

    public Resources loadDefaultResources() {
        return Resources.fromFile(getResourcesFile(DEFAULT_LOCALE));
    }

    public Resources loadResources(@NonNull String locale) {
        return Resources.fromFile(getResourcesFile(locale));
    }

    // Applies the default resources as a template for the given locale's
    // resources, returning the result as a String (might be null if failed)
    public String applyDefaultTemplate(String locale) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (applyDefaultTemplate(locale, out))
            return out.toString();
        else
            return null;
    }

    // Applies the default resources as a template for the given
    // locale's resources, outputting the result to the given stream
    public boolean applyDefaultTemplate(String locale, OutputStream out) {
        if (!hasLocale(locale))
            return false;

        File defaultFile = getResourcesFile(DEFAULT_LOCALE);
        if (!defaultFile.isFile())
            return false;

        return ResourcesParser.applyTemplate(defaultFile,
                loadResources(locale), out);
    }

    //endregion

    //endregion

    //region Static repository listing

    public static ArrayList<RepoHandler> listRepositories(Context context) {
        ArrayList<RepoHandler> repositories = new ArrayList<>();

        File root = new File(context.getFilesDir(), BASE_DIR);
        if (root.isDirectory()) {
            for (File f : root.listFiles()) {
                if (f.isDirectory()) {
                    repositories.add(new RepoHandler(context, f));
                }
            }
        }

        return repositories;
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
                    "Only repositories with a GitHub url can be converted to owner and repository");
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
}
