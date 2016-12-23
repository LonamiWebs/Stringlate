package io.github.lonamiwebs.stringlate.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Settings {
    private final SharedPreferences mPrefs;

    private static final String KEY_DOWNLOAD_ICONS = "download_icons";

    private static final Boolean DEFAULT_DOWNLOAD_ICONS = false;

    //region Constructor

    public Settings(final Context context) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    //endregion

    //region Getters

    public boolean isDownloadIconsAllowed() {
        return mPrefs.getBoolean(KEY_DOWNLOAD_ICONS, DEFAULT_DOWNLOAD_ICONS);
    }

    //endregion

    //region Setters

    public void setDownloadIconsAllowed(final boolean download) {
        mPrefs.edit().putBoolean(KEY_DOWNLOAD_ICONS, download).apply();
    }

    //endregion
}
