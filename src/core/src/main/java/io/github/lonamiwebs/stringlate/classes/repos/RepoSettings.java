package io.github.lonamiwebs.stringlate.classes.repos;

import net.gsantner.opoc.util.FileUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import io.github.lonamiwebs.stringlate.classes.sources.SourceSettings;

// We can't quite save the SharedPreferences in a custom path so… use JSON (easier than XML)
public class RepoSettings {

    private static final String FILENAME = "settings.json";
    private final File mSettingsFile;
    private JSONObject mSettings;

    private static final String KEY_SOURCE = "source";
    private static final String KEY_PROJECT_WEB_URL = "project_homepage_url";
    private static final String KEY_LAST_LOCALE = "last_locale";
    private static final String KEY_REMOTE_PATHS = "remote_paths";
    private static final String KEY_ICON_PATH = "icon_path";
    private static final String KEY_SEARCH_FILTER = "search_filter";
    private static final String KEY_CREATED_ISSUES = "created_issues";
    private static final String KEY_PROJECT_NAME = "project_name";
    private static final String KEY_PROJECT_MAIL = "project_mail";

    private static final String DEFAULT_LAST_LOCALE = "";
    private static final String DEFAULT_PROJECT_NAME = "";
    private static final String DEFAULT_PROJECT_MAIL = "";

    //region Constructor

    public RepoSettings(final File repoDir) {
        mSettingsFile = new File(repoDir, FILENAME);
        mSettings = load();
    }

    //endregion

    // TODO Remove by version 1.0 or so
    public void checkUpgradeSettingsToSpecific(final SourceSettings sourceSettings) {
        if (mSettings.has("git_url")) {
            // Name change: "git_url" -> "source"
            setSource(mSettings.optString("git_url", ""));

            // Location change: RepoSettings -> git-specific SourceSettings (all if upgrading)
            sourceSettings.set("translation_service", mSettings.optString("translation_service"));
            try {
                final ArrayList<String> branchesArray = new ArrayList<>();
                JSONArray branches = mSettings.optJSONArray("remote_branches");
                if (branches != null) {
                    for (int i = 0; i < branches.length(); ++i) {
                        branchesArray.add(branches.getString(i));
                    }
                    sourceSettings.setArray("remote_branches", branchesArray);
                }
            } catch (JSONException ignored) {
            }

            mSettings.remove("git_url");
            mSettings.remove("translation_service");
            mSettings.remove("remote_branches");
            save();
        }
    }

    //region Getters

    public String getSource() {
        return mSettings.optString(KEY_SOURCE, "");
    }

    public String getProjectWebUrl() {
        return mSettings.optString(KEY_PROJECT_WEB_URL, getSource());
    }

    public String getProjectName() {
        return mSettings.optString(KEY_PROJECT_NAME, DEFAULT_PROJECT_NAME);
    }

    public String getProjectMail() {
        return mSettings.optString(KEY_PROJECT_MAIL, DEFAULT_PROJECT_MAIL);
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
            } catch (JSONException ignored) {
            }
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
            } catch (JSONException ignored) {
            }
        }
        return map;
    }

    //endregion

    //region Setters

    public void setSource(final String source) {
        try {
            mSettings.put(KEY_SOURCE, source);
        } catch (JSONException ignored) {
        }
        save();
    }

    public void setProjectWebUrl(final String homepageUrl) {
        try {
            mSettings.put(KEY_PROJECT_WEB_URL, homepageUrl);
        } catch (JSONException ignored) {
        }
        save();
    }

    public void setProjectName(final String projectName) {
        try {
            mSettings.put(KEY_PROJECT_NAME, projectName);
        } catch (JSONException ignored) {
        }
        save();
    }

    public void setProjectMail(final String projectMail) {
        try {
            mSettings.put(KEY_PROJECT_MAIL, projectMail);
        } catch (JSONException ignored) {
        }
        save();
    }

    public void setLastLocale(String locale) {
        try {
            mSettings.put(KEY_LAST_LOCALE, locale);
        } catch (JSONException ignored) {
        }
        save();
    }

    public void addRemotePath(String filename, String remotePath) {
        try {
            HashMap<String, String> map = getRemotePaths();
            map.put(filename, remotePath);
            mSettings.put(KEY_REMOTE_PATHS, new JSONObject(map));
        } catch (JSONException ignored) {
        }
        save();
    }

    public void clearRemotePaths() {
        mSettings.remove(KEY_REMOTE_PATHS);
    }

    public void setIconFile(File file) {
        try {
            mSettings.put(KEY_ICON_PATH, file == null ? "" : file.getAbsolutePath());
        } catch (JSONException ignored) {
        }
        save();
    }

    public void setStringFilter(final String filter) {
        if (filter == null)
            throw new IllegalArgumentException();
        try {
            mSettings.put(KEY_SEARCH_FILTER, filter);
        } catch (JSONException ignored) {
        }
        save();
    }

    public void addCreatedIssue(String locale, int issueNumber) {
        try {
            HashMap<String, Integer> map = getCreatedIssues();
            map.put(locale, issueNumber);
            mSettings.put(KEY_CREATED_ISSUES, new JSONObject(map));
        } catch (JSONException ignored) {
        }
        save();
    }

    //endregion

    //region Load/save

    private JSONObject load() {
        try {
            final String json = FileUtils.readTextFile(mSettingsFile);
            if (!json.isEmpty())
                return new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }

    public boolean save() {
        return FileUtils.writeFile(mSettingsFile, mSettings.toString());
    }

    //endregion
}
