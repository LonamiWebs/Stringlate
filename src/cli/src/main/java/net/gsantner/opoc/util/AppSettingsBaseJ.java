/*
 * ------------------------------------------------------------------------------
 * Gregor Santner <gsantner.github.io> wrote this. You can do whatever you want
 * with it. If we meet some day, and you think it is worth it, you can buy me a
 * coke in return. Provided as is without any kind of warranty. Do not blame or
 * sue me if something goes wrong. No attribution required.    - Gregor Santner
 *
 * License: Creative Commons Zero (CC0 1.0)
 *  http://creativecommons.org/publicdomain/zero/1.0/
 * ----------------------------------------------------------------------------
 */
package net.gsantner.opoc.util;

import java.util.Locale;
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

    private synchronized Preferences getPref() {
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
        String ret;
        String os = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if ((os.contains("mac")) || (os.contains("darwin"))) {
            ret = "~/Library/Preferences/%s";
        } else if (os.contains("win")) {
            ret = "HKEY_CURRENT_USER\\Software\\%s";
        } else {
            ret = System.getProperty("user.home") + "/.java/.userPrefs/%s/prefs.xml";
        }
        return String.format(ret, getPref().absolutePath().substring(1));
    }

    public boolean isAutoClose() {
        return _isAutoClose;
    }

    public void setAutoClose(boolean autoClose) {
        _isAutoClose = autoClose;
    }

    /**
     * Get an int value
     *
     * @param key          the key
     * @param defaultValue the default value
     * @return the value or defaultValue if inexistient
     */
    public int getInt(String key, int defaultValue) {
        int ret = getPref().getInt(key, defaultValue);
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
    public void setInt(String key, int value) {
        getPref().putInt(key, value);
        if (_isAutoClose) {
            closePrefs();
        }
    }

    /**
     * Get an String value
     *
     * @param key          the key
     * @param defaultValue the default value
     * @return the value or defaultValue if inexistient
     */
    public String getString(String key, String defaultValue) {
        String ret = getPref().get(key, defaultValue);
        if (_isAutoClose) {
            closePrefs();
        }
        return ret;
    }

    /**
     * Set an String value
     *
     * @param key   the key
     * @param value the value
     */
    public void setString(String key, String value) {
        getPref().put(key, value);
        if (_isAutoClose) {
            closePrefs();
        }
    }

    /**
     * Get an int value
     *
     * @param key          the key
     * @param defaultValue the default value
     * @return the value or defaultValue if inexistient
     */
    public boolean getBool(String key, boolean defaultValue) {
        boolean ret = getPref().getBoolean(key, defaultValue);
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
    public void setBool(String key, boolean value) {
        getPref().putBoolean(key, value);
        if (_isAutoClose) {
            closePrefs();
        }
    }

    public void reset(boolean setDefaultOptions) {
        try {
            getPref().clear();
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
