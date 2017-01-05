package io.github.lonamiwebs.stringlate.settings;

import android.content.Context;
import android.support.annotation.NonNull;

import static io.github.lonamiwebs.stringlate.utilities.Constants.GITHUB_WANTED_SCOPES;

public class AppSettings extends Settings {

    private static final String KEY_DOWNLOAD_ICONS = "download_icons";
    private static final String KEY_GITHUB_TOKEN = "github_access_token";
    private static final String KEY_GITHUB_SCOPE = "github_access_scope";

    private static final Boolean DEFAULT_DOWNLOAD_ICONS = false;
    private static final String DEFAULT_GITHUB_TOKEN = "";
    private static final String DEFAULT_GITHUB_SCOPE = "";

    //region Constructor

    public AppSettings(final Context context) {
        super(context, null);
    }

    //endregion

    //region Getters

    public boolean isDownloadIconsAllowed() {
        return getSettings().getBoolean(KEY_DOWNLOAD_ICONS, DEFAULT_DOWNLOAD_ICONS);
    }

    // Tokens seem to be saved OK as shared preferences http://stackoverflow.com/q/10161266
    // Another option, AccountManager: http://stackoverflow.com/q/14437096
    @NonNull public String getGitHubToken() {
        return getSettings().getString(KEY_GITHUB_TOKEN, DEFAULT_GITHUB_TOKEN);
    }

    @NonNull private String[] getGitHubScopes() {
        return getSettings().getString(KEY_GITHUB_SCOPE, DEFAULT_GITHUB_SCOPE).split(",");
    }

    public boolean hasGitHubAuthorization() {
        return !getGitHubToken().isEmpty() &&
                getGitHubScopes().length == GITHUB_WANTED_SCOPES.length;
    }

    //endregion

    //region Setters

    public void setDownloadIconsAllowed(final boolean download) {
        editSettings().putBoolean(KEY_DOWNLOAD_ICONS, download).apply();
    }

    public void setGitHubAccess(String token, String scope) {
        editSettings().putString(KEY_GITHUB_TOKEN, token).apply();
        editSettings().putString(KEY_GITHUB_SCOPE, scope).apply();
    }

    //endregion
}
