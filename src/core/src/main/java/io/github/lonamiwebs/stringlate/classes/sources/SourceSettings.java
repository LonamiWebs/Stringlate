package io.github.lonamiwebs.stringlate.classes.sources;

import net.gsantner.opoc.util.FileUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

// Custom settings that different StringsSource may need
public class SourceSettings {

    private static final String FILENAME = "source-settings.json";
    private final File mSettingsFile;
    private JSONObject mSettings;

    private static final String KEY_NAME = "_name";
    private static final String DEFAULT_NAME = "";

    //region Constructor

    public SourceSettings(final File directory) {
        mSettingsFile = new File(directory, FILENAME);
        mSettings = load();
    }

    //endregion

    //region Getters

    public String getName() {
        return mSettings.optString(KEY_NAME, DEFAULT_NAME);
    }

    public Object get(final String name) {
        return mSettings.opt(name);
    }

    @SuppressWarnings("unchecked")
    public <T> ArrayList<T> getArray(final String name) {
        final ArrayList<T> result = new ArrayList<>();
        try {
            JSONArray array = mSettings.optJSONArray(name);
            if (array == null)
                return result;

            for (int i = 0; i < array.length(); ++i) {
                try {
                    result.add((T) array.get(i));
                } catch (ClassCastException ignored) {
                    System.err.println("SourceSettings: Failed to load array item for setting " + name + ": " + array.get(i));
                }
            }
        } catch (JSONException ignored) {
        }
        return result;
    }

    //endregion

    //region Setters

    public void setName(final String name) {
        try {
            mSettings.put(KEY_NAME, name);
        } catch (JSONException ignored) {
        }
        save();
    }

    public void set(final String name, final Object object) {
        if (name.startsWith("_"))
            throw new IllegalArgumentException("Names for the source settings cannot start with underscore (_).");
        try {
            mSettings.put(name, object);
        } catch (JSONException ignored) {
        }
        save();
    }

    public void setArray(final String name, final ArrayList objects) {
        if (name.startsWith("_"))
            throw new IllegalArgumentException("Names for the source settings cannot start with underscore (_).");
        try {
            JSONArray array = new JSONArray();
            for (Object branch : objects)
                array.put(branch);
            mSettings.put(name, array);
        } catch (JSONException ignored) {
        }
        save();
    }

    //endregion

    //region Load/save/reset

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

    public void reset(final String newName) {
        mSettings = new JSONObject();
        setName(newName);
    }

    //endregion
}
