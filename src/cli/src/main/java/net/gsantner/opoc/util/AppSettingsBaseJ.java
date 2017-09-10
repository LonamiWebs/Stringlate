package net.gsantner.opoc.util;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

@SuppressWarnings({"WeakerAccess", "unused"})
public class AppSettingsBaseJ {
    private Class _classOfApplicationPackage;
    Preferences _preference;
    boolean _isAutoClose;

    public AppSettingsBaseJ(Class classOfApplicationPackage) {
        _classOfApplicationPackage = classOfApplicationPackage;
    }

    private synchronized Preferences loadPref() {
        if (_preference == null) {
            _preference = Preferences.userNodeForPackage(_classOfApplicationPackage);
        }
        return _preference;
    }

    public synchronized void closePrefs() {
        if (_preference != null) {
            try {
                _preference.sync();
            } catch (BackingStoreException e) {
                e.printStackTrace();
            }
        }
        _preference = null;
    }

    public String getPathToPropertiesFile() {
        return "~/.java/.userPrefs" + loadPref().absolutePath() + "/prefs.xml";
    }

    public boolean isAutoClose() {
        return _isAutoClose;
    }

    public void setAutoClose(boolean autoClose) {
        this._isAutoClose = autoClose;
    }

    /**
     * Get an int value
     *
     * @param key          the key
     * @param defaultValue the default value
     * @return the value or defaultValue if inexistient
     */
    private int getInt(String key, int defaultValue) {
        int ret = loadPref().getInt(key, defaultValue);
        if (_isAutoClose) {
            closePrefs();
        }
        return ret;
    }

    /**
     * Get an String value
     *
     * @param key          the key
     * @param defaultValue the default value
     * @return the value or defaultValue if inexistient
     */
    private String getString(String key, String defaultValue) {
        String ret = loadPref().get(key, defaultValue);
        if (_isAutoClose) {
            closePrefs();
        }
        return ret;
    }

    /**
     * Set an int value
     *
     * @param key   the key
     * @param value the value
     */
    private void setInt(String key, int value) {
        loadPref().putInt(key, value);
        if (_isAutoClose) {
            closePrefs();
        }
    }

    /**
     * Set an String value
     *
     * @param key   the key
     * @param value the value
     */
    private void setString(String key, String value) {
        loadPref().put(key, value);
        if (_isAutoClose) {
            closePrefs();
        }
    }

    public void reset(boolean setDefaultOptions) {
        try {
            loadPref().clear();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }

        if (setDefaultOptions) {
            resetDefaultValues();
        }
    }

    protected void resetDefaultValues() {

    }
}
