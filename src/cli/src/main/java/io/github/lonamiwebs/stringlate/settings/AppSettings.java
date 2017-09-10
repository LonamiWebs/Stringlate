package io.github.lonamiwebs.stringlate.settings;

import net.gsantner.opoc.util.AppSettingsBaseJ;

import io.github.lonamiwebs.stringlate.interfaces.SlAppSettings;

public class AppSettings extends AppSettingsBaseJ implements SlAppSettings {
    private static final String KEY_DOWNLOAD_ICONS = "download_icons";
    private static final String KEY_GITHUB_TOKEN = "github_access_token";
    private static final String KEY_GITHUB_SCOPE = "github_access_scope";
    private static final String KEY_STRING_SORTING = "string_sorting";

    public AppSettings() {
        super(io.github.lonamiwebs.stringlate.cli.Main.class);
    }

    @Override
    public boolean isDownloadIconsAllowed() {
        return getBool(KEY_DOWNLOAD_ICONS, DEFAULT_DOWNLOAD_ICONS);
    }

    @Override
    public String getGitHubToken() {
        return getString(KEY_GITHUB_TOKEN, DEFAULT_GITHUB_TOKEN);
    }

    @Override
    public boolean hasGitHubAuthorization() {
        return !getGitHubToken().isEmpty() &&
                getGitHubScopes().length == 2;
    }

    @Override
    public void setDownloadIconsAllowed(boolean value) {
        setBool(KEY_DOWNLOAD_ICONS, value);
    }

    @Override
    public void setGitHubAccess(String token, String scope) {
        setString(KEY_GITHUB_TOKEN, token);
        setString(KEY_GITHUB_SCOPE, scope);
    }

    @Override
    public void setStringSortMode(int value) {
        setInt(KEY_STRING_SORTING, value);
    }

    @Override
    public String getLanguage() {
        return "";
    }

    @Override
    public int getStringSortMode() {
        return getInt(KEY_STRING_SORTING, DEFAULT_STRING_SORTING);
    }

    @Override
    public String[] getGitHubScopes() {
        return getString(KEY_GITHUB_SCOPE, DEFAULT_GITHUB_SCOPE).split(",");
    }

}
