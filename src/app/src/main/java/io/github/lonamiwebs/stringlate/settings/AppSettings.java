package io.github.lonamiwebs.stringlate.settings;

import android.content.Context;

public class AppSettings extends Settings {

    private static final String KEY_DOWNLOAD_ICONS = "download_icons";

    private static final Boolean DEFAULT_DOWNLOAD_ICONS = false;

    //region Constructor

    public AppSettings(final Context context) {
        super(context, null);
    }

    //endregion

    //region Getters

    public boolean isDownloadIconsAllowed() {
        return getSettings().getBoolean(KEY_DOWNLOAD_ICONS, DEFAULT_DOWNLOAD_ICONS);
    }

    //endregion

    //region Setters

    public void setDownloadIconsAllowed(final boolean download) {
        editSettings().putBoolean(KEY_DOWNLOAD_ICONS, download).apply();
    }

    //endregion
}
