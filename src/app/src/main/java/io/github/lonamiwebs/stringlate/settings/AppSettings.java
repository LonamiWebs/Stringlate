package io.github.lonamiwebs.stringlate.settings;

import android.content.Context;
import android.support.annotation.NonNull;

import net.gsantner.opoc.util.AppSettingsBase;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.classes.resources.ResourceStringComparator;
import io.github.lonamiwebs.stringlate.interfaces.SlAppSettings;

import static io.github.lonamiwebs.stringlate.utilities.Constants.GITHUB_WANTED_SCOPES;

public class AppSettings extends AppSettingsBase implements SlAppSettings {
    private static final String KEY_DOWNLOAD_ICONS = "download_icons";
    private static final String KEY_GITHUB_TOKEN = "github_access_token";
    private static final String KEY_GITHUB_SCOPE = "github_access_scope";
    private static final String KEY_STRING_SORTING = "string_sorting";

    private static final Boolean DEFAULT_DOWNLOAD_ICONS = false;
    private static final String DEFAULT_GITHUB_TOKEN = "";
    private static final String DEFAULT_GITHUB_SCOPE = "";
    private static final int DEFAULT_STRING_SORTING = ResourceStringComparator.SORT_ALPHABETICALLY;

    //region Constructor

    public AppSettings(final Context context) {
        super(context, null);
    }

    //endregion

    //region Getters

    public boolean isDownloadIconsAllowed() {
        return getBool(KEY_DOWNLOAD_ICONS, DEFAULT_DOWNLOAD_ICONS);
    }

    // Tokens seem to be saved OK as shared preferences http://stackoverflow.com/q/10161266
    // Another option, AccountManager: http://stackoverflow.com/q/14437096
    @NonNull
    public String getGitHubToken() {
        return getString(KEY_GITHUB_TOKEN, DEFAULT_GITHUB_TOKEN);
    }

    @NonNull
    private String[] getGitHubScopes() {
        return getString(KEY_GITHUB_SCOPE, DEFAULT_GITHUB_SCOPE).split(",");
    }

    public boolean hasGitHubAuthorization() {
        return !getGitHubToken().isEmpty() &&
                getGitHubScopes().length == GITHUB_WANTED_SCOPES.length;
    }

    public int getStringSortMode() {
        return getInt(KEY_STRING_SORTING, DEFAULT_STRING_SORTING);
    }

    //endregion

    //region Setters

    public void setDownloadIconsAllowed(final boolean download) {
        setBool(KEY_DOWNLOAD_ICONS, download);
    }

    public void setGitHubAccess(String token, String scope) {
        setString(KEY_GITHUB_TOKEN, token);
        setString(KEY_GITHUB_SCOPE, scope);
    }

    public void setStringSortMode(int mode) {
        setInt(KEY_STRING_SORTING, mode);
    }

    public String getLanguage() {
        return getString(R.string.pref_key__language, "");
    }

    //endregion
}
