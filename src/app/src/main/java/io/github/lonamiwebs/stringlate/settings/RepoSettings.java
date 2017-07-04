package io.github.lonamiwebs.stringlate.settings;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.support.annotation.NonNull;

import io.github.lonamiwebs.stringlate.utilities.Utils;

// We can't quite save the SharedPreferences in a custom path so… use JSON (easier than XML)
public class RepoSettings {

    private static final String FILENAME = "settings.json";
    private final File mSettingsFile;
    private JSONObject mSettings;

    private static final String KEY_GIT_URL = "git_url";
    private static final String KEY_LAST_LOCALE = "last_locale";
    private static final String KEY_REMOTE_PATHS = "remote_paths";
    private static final String KEY_ICON_PATH = "icon_path";
    private static final String KEY_SEARCH_FILTER = "search_filter";
    private static final String KEY_CREATED_ISSUES = "created_issues";
    private static final String KEY_USED_TRANSLATION_SERVICE = "translation_service";
    private static final String KEY_REMOTE_BRANCHES = "remote_branches";

    private static final String DEFAULT_GIT_URL = "";
    private static final String DEFAULT_LAST_LOCALE = null;

    //region Constructor

    public RepoSettings(final File repoDir) {
        mSettingsFile = new File(repoDir, FILENAME);
        mSettings = load();
    }

    //endregion

    //region Getters

    @NonNull
    public String getGitUrl() {
        return mSettings.optString(KEY_GIT_URL, DEFAULT_GIT_URL);
    }

    public String getLastLocale() {
        return mSettings.optString(KEY_LAST_LOCALE, DEFAULT_LAST_LOCALE);
    }

    public HashMap<String, String> getRemotePaths() {
        HashMap<String, String> map = new HashMap<>();
        JSONObject json = mSettings.optJSONObject(KEY_REMOTE_PATHS);
        if (json != null) {
            try {
                Iterator<String> keysItr = json.keys();
                while (keysItr.hasNext()) {
                    String key = keysItr.next();
                    map.put(key, json.getString(key));
                }
            } catch (JSONException ignored) { }
        }
        return map;
    }

    public static boolean exists(final File repoDir) {
        return new File(repoDir, FILENAME).isFile();
    }

    public File getIconFile() {
        String path = mSettings.optString(KEY_ICON_PATH, "");
        if (path.isEmpty())
            return null;

        File result = new File(path);
        return result.isFile() ? result : null;
    }

    @NonNull
    public String getUsedTranslationService() {
        return mSettings.optString(KEY_USED_TRANSLATION_SERVICE, "");
    }

    @NonNull
    public String getStringFilter() {
        return mSettings.optString(KEY_SEARCH_FILTER, "");
    }

    // HashMap<Locale string, GitHub issue number>
    public HashMap<String, Integer> getCreatedIssues() {
        HashMap<String, Integer> map = new HashMap<>();
        JSONObject json = mSettings.optJSONObject(KEY_CREATED_ISSUES);
        if (json != null) {
            try {
                Iterator<String> keysItr = json.keys();
                while (keysItr.hasNext()) {
                    String key = keysItr.next();
                    map.put(key, json.getInt(key));
                }
            } catch (JSONException ignored) { }
        }
        return map;
    }

    @NonNull
    public ArrayList<String> getRemoteBranches() {
        final ArrayList<String> result = new ArrayList<>();
        try {
            JSONArray branches = mSettings.getJSONArray(KEY_REMOTE_BRANCHES);
            for (int i = 0; i < branches.length(); ++i) {
                result.add(branches.getString(i));
            }
        } catch (JSONException ignored) { }
        return result;
    }

    //endregion

    //region Setters

    public void setGitUrl(final String gitUrl) {
        try { mSettings.put(KEY_GIT_URL, gitUrl); }
        catch (JSONException ignored) { }
        save();
    }

    public void setLastLocale(String locale) {
        try { mSettings.put(KEY_LAST_LOCALE, locale); }
        catch (JSONException ignored) { }
        save();
    }

    public void addRemotePath(String filename, String remotePath) {
        try {
            HashMap<String, String> map = getRemotePaths();
            map.put(filename, remotePath);
            mSettings.put(KEY_REMOTE_PATHS, new JSONObject(map));
        }
        catch (JSONException ignored) { }
        save();
    }

    public void clearRemotePaths() {
        mSettings.remove(KEY_REMOTE_PATHS);
    }

    public void setIconFile(File file) {
        try { mSettings.put(KEY_ICON_PATH, file == null ? "" : file.getAbsolutePath()); }
        catch (JSONException ignored) { }
        save();
    }

    public void setUsedTranslationService(@NonNull final String service) {
        try { mSettings.put(KEY_USED_TRANSLATION_SERVICE, service); }
        catch (JSONException ignored) { }
        save();
    }

    public void setStringFilter(@NonNull final String filter) {
        try { mSettings.put(KEY_SEARCH_FILTER, filter); }
        catch (JSONException ignored) { }
        save();
    }

    public void addCreatedIssue(String locale, int issueNumber) {
        try {
            HashMap<String, Integer> map = getCreatedIssues();
            map.put(locale, issueNumber);
            mSettings.put(KEY_CREATED_ISSUES, new JSONObject(map));
        }
        catch (JSONException ignored) { }
        save();
    }

    public void setRemoteBranches(final ArrayList<String> branches) {
        try {
            JSONArray array = new JSONArray();
            for (String branch : branches)
                array.put(branch);
            mSettings.put(KEY_REMOTE_BRANCHES, array);
        }
        catch (JSONException ignored) { }
        save();
    }

    //endregion

    //region Load/save

    @NonNull private JSONObject load() {
        try {
            final String json = Utils.readFile(mSettingsFile);
            if (!json.isEmpty())
                return new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }

    public boolean save() {
        return Utils.writeFile(mSettingsFile, mSettings.toString());
    }

    //endregion
}
