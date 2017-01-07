package io.github.lonamiwebs.stringlate.settings;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

// We can't quite save the SharedPreferences in a custom path so… use JSON (easier than XML)
public class RepoSettings {

    private static final String FILENAME = "settings.json";
    private final File mSettingsFile;
    private JSONObject mSettings;

    private static final String KEY_GIT_URL = "git_url";
    private static final String KEY_LAST_LOCALE = "last_locale";
    private static final String KEY_REMOTE_PATHS = "remote_paths";

    private static final String DEFAULT_GIT_URL = "";
    private static final String DEFAULT_LAST_LOCALE = null;

    //region Constructor

    public RepoSettings(final File repoDir) {
        mSettingsFile = new File(repoDir, FILENAME);
        load();
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

    private HashMap<String, String> getRemotePaths() {
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

    //endregion

    //region Load/save

    public void load() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(mSettingsFile));
            mSettings = new JSONObject(reader.readLine());
        } catch (FileNotFoundException ignored) {
            mSettings = new JSONObject();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            mSettings = new JSONObject();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean save() {
        BufferedWriter writer = null;
        try {
            if (!mSettingsFile.getParentFile().isDirectory())
                mSettingsFile.getParentFile().mkdirs();

            writer = new BufferedWriter(new FileWriter(mSettingsFile));
            writer.write(mSettings.toString());
            writer.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //endregion
}
