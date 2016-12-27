package io.github.lonamiwebs.stringlate.utilities;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.resources.Resources;
import io.github.lonamiwebs.stringlate.classes.resources.ResourcesParser;
import io.github.lonamiwebs.stringlate.classes.resources.ResourcesString;
import io.github.lonamiwebs.stringlate.interfaces.Callback;
import io.github.lonamiwebs.stringlate.interfaces.ProgressUpdateCallback;
import io.github.lonamiwebs.stringlate.settings.Settings;

// Class used to inter-operate with locally saved GitHub "repositories"
// What is stored are simply the strings.xml file under a tree directory structure:
/*
owner1/
       repo1/
             strings.xml
             strings-es.xml
       repo2/
             ...
owner2/
       ...
* */
public class RepoHandler extends Settings {

    //region Members

    private final Context mContext;
    private final String mOwner, mRepo;

    private final File mRoot;

    private final Pattern mValuesLocalePattern; // Match locale from "res/values-(...)/strings.xml"
    private final Pattern mLocalesPattern; // Match locale from "strings-(...).xml"
    private final ArrayList<String> mLocales;

    private static final String BASE_DIR = "repos";
    private static final String RAW_FILE_URL = "https://raw.githubusercontent.com/%s/%s/master/%s";

    private static final String GITHUB_REPO_URL = "https://github.com/%s/%s";

    public static final String DEFAULT_LOCALE = "default";

    private static final String SETTINGS_FORMAT = "io.github.lonamiwebs.stringlate.repo.%s.%s";

    //endregion

    //region Interfaces and events

    public interface ChangeListener {
        // Called when a repository is either added or removed
        void onRepositoryCountChanged();
    }

    private static ArrayList<ChangeListener> mChangeListeners = new ArrayList<>();

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
        return new RepoHandler(context, bundle.getString("owner"), bundle.getString("repo"));
    }

    public RepoHandler(Context context, String owner, String repo) {
        super(context, String.format(SETTINGS_FORMAT, owner, repo));
        mContext = context;
        mOwner = owner;
        mRepo = repo;

        mRoot = new File(mContext.getFilesDir(), BASE_DIR+"/"+mOwner+"/"+mRepo);

        mValuesLocalePattern = Pattern.compile(
                "res/values(?:-([\\w-]+))?/strings\\.xml");

        mLocalesPattern = Pattern.compile("strings(?:-([\\w-]+))?\\.xml");
        mLocales = new ArrayList<>();
        loadLocales();
    }

    //endregion

    //region Utilities

    // Retrieves the File object for the given locale
    private File getResourcesFile(String locale) {
        if (locale == null || locale.equals(DEFAULT_LOCALE))
            return new File(mRoot, "strings.xml");
        else
            return new File(mRoot, "strings-"+locale+".xml");
    }

    // Determines whether a given locale is saved or not
    public boolean hasLocale(String locale) { return getResourcesFile(locale).isFile(); }

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
    public void delete() {
        for (File file : mRoot.listFiles())
            file.delete();

        mRoot.delete();
        if (mRoot.getParentFile().listFiles().length == 0)
            mRoot.getParentFile().delete();

        notifyRepositoryCountChanged();
    }

    //endregion

    //region Locales

    //region Loading locale files

    private void loadLocales() {
        mLocales.clear();
        if (mRoot.isDirectory()) {
            for (File f : mRoot.listFiles()) {
                String path = f.getAbsolutePath();
                Matcher m = mLocalesPattern.matcher(path);
                if (m.find())
                    mLocales.add(m.group(1) == null ? DEFAULT_LOCALE : m.group(1));
            }
        }
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
        scanStringsXml(callback, keepChanges);
    }

    // Step 1: Scans the current repository to find strings.xml files
    //         If any file is found, its remote path and locale name is added to a list,
    //         and the Step 2. is called (downloadLocales).
    private void scanStringsXml(final ProgressUpdateCallback callback, final boolean keepChanges) {
        callback.onProgressUpdate(
                mContext.getString(R.string.scanning_repository),
                mContext.getString(R.string.scanning_repository_long));

        // We want to find files on the owner/repo repository
        // containing 'resources' ('<resources>') on them and the filename
        // being 'strings.xml'. Some day Java will have named parameters...
        GitHub.gFindFiles(mOwner, mRepo, "resources", "strings.xml", new Callback<Object>() {
            @Override
            public void onCallback(Object o) {
                ArrayList<String> remotePaths = new ArrayList<>();
                ArrayList<String> locales = new ArrayList<>();
                try {
                    JSONObject json = (JSONObject) o;
                    JSONArray items = json.getJSONArray("items");
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        Matcher m = mValuesLocalePattern.matcher(item.getString("path"));
                        if (m.find()) {
                            remotePaths.add(item.getString("path"));
                            locales.add(m.group(1) == null ? DEFAULT_LOCALE : m.group(1));
                        }
                    }
                    if (remotePaths.size() == 0) {
                        callback.onProgressFinished(
                                mContext.getString(R.string.no_strings_found), false);
                    } else {
                        downloadLocales(remotePaths, locales, keepChanges, callback);
                    }
                } catch (JSONException e) {
                    callback.onProgressFinished(
                            mContext.getString(R.string.error_parsing_json), false);
                }
            }
        });
    }

    // Step 2: Given the remote paths of the strings.xml files,
    //         download them to our local "repository".
    private void downloadLocales(final ArrayList<String> remotePaths,
                                 final ArrayList<String> locales,
                                 final boolean keepChanges,
                                 final ProgressUpdateCallback callback) {
        callback.onProgressUpdate(
                mContext.getString(R.string.downloading_strings_locale, 0, remotePaths.size()),
                mContext.getString(R.string.downloading_to_translate));

        new AsyncTask<Void, Integer, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                for (int i = 0; i < remotePaths.size(); i++) {
                    publishProgress(i);
                    downloadLocale(remotePaths.get(i), locales.get(i), keepChanges);
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                int i = values[0];
                callback.onProgressUpdate(
                        mContext.getString(R.string.downloading_strings_locale, i+1, remotePaths.size()),
                        mContext.getString(R.string.downloading_strings_locale_description, locales.get(i)));

                super.onProgressUpdate(values);
            }

            @Override
            protected void onPostExecute(Void v) {
                loadLocales();
                callback.onProgressFinished(null, true);
                notifyRepositoryCountChanged();
                super.onPostExecute(v);
            }
        }.execute();
    }

    // Downloads a single locale file to our local "repository"
    public void downloadLocale(String remotePath, String locale, boolean keepChanges) {
        final String urlString = String.format(RAW_FILE_URL, mOwner, mRepo, remotePath);
        final File outputFile = getResourcesFile(locale);

        if (keepChanges) {
            final ArrayList<ResourcesString> strings = new ArrayList<>();
            // Load in memory all the previous resource strings for this locale.
            // After the remote file has been downloaded, add our modifications
            // to this new file and save it again, so our changes are conserved.
            Resources resources = Resources.fromFile(outputFile);
            if (resources != null) {
                for (ResourcesString rs : resources) {
                    if (rs.wasModified())
                        strings.add(rs);
                }
            }

            FileDownloader.downloadFile(urlString, outputFile);

            // Now load the remote file from the repository
            // and set the modified contents from the old file
            resources = Resources.fromFile(outputFile);
            if (resources != null) {
                for (ResourcesString rs : strings) {
                    // Perhaps the remote files equal our modified content
                    // In that case, .setContent() will do nothing and the
                    // new string will remain as non-modified
                    resources.setContent(rs.getId(), rs.getContent());
                }
                resources.save();
            }
        } else {
            // Don't keep any change. Simply download the remote file
            FileDownloader.downloadFile(urlString, outputFile);
        }
    }

    //endregion

    //region Loading resources

    public Resources loadResources(String locale) {
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

        File defaultFile = getResourcesFile(null);
        if (!defaultFile.isFile())
            return false;

        return ResourcesParser.applyTemplate(defaultFile,
                loadResources(locale), out);
    }

    //endregion

    //endregion

    //region Static repository listing

    // Lists all the owners under the base directory
    public static ArrayList<String> listOwners(Context context) {
        ArrayList<String> owners = new ArrayList<>();

        File root = new File(context.getFilesDir(), BASE_DIR);
        if (root.isDirectory())
            for (File f : root.listFiles())
                if (f.isDirectory())
                    owners.add(f.getName());

        return owners;
    }

    // Lists all the repositories of the given owner
    public static ArrayList<String> listRepositories(Context context, String owner) {
        ArrayList<String> repositories = new ArrayList<>();

        File root = new File(context.getFilesDir(), BASE_DIR+"/"+owner);
        if (root.isDirectory())
            for (File f : root.listFiles())
                if (f.isDirectory())
                    repositories.add(f.getName());

        return repositories;
    }

    // Lists all the repositories of all the owners under the base directory
    // and returns a list to their URLs at GitHub
    public static ArrayList<String> listRepositories(Context context, boolean prefixUrl) {
        ArrayList<String> repositories = new ArrayList<>();
        if (prefixUrl) {
            for (String owner : listOwners(context)) {
                for (String repository : listRepositories(context, owner)) {
                    repositories.add(String.format(GITHUB_REPO_URL, owner, repository));
                }
            }
        } else {
            for (String owner : listOwners(context)) {
                for (String repository : listRepositories(context, owner)) {
                    repositories.add(owner+"/"+repository);
                }
            }
        }
        return repositories;
    }

    //endregion

    //region Settings

    //region Settings keys

    private static final String KEY_LAST_LOCALE = "last_locale";

    private static final String DEFAULT_LAST_LOCALE = null;

    //endregion

    //region Getting settings

    public String getLastLocale() {
        return getSettings().getString(KEY_LAST_LOCALE, DEFAULT_LAST_LOCALE);
    }

    //endregion

    //region Setting settings

    public void setLastLocale(String locale) {
        editSettings().putString(KEY_LAST_LOCALE, locale).apply();
    }

    //endregion

    //endregion

    //region To other objects

    @Override
    public String toString() {
        return mOwner+"/"+mRepo;
    }

    public String toString(boolean onlyRepo) {
        return onlyRepo ? mRepo : toString();
    }

    public Bundle toBundle() {
        Bundle result = new Bundle();
        result.putString("owner", mOwner);
        result.putString("repo", mRepo);
        return result;
    }

    //endregion
}
