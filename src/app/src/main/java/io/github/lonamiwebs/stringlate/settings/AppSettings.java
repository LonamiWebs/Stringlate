package io.github.lonamiwebs.stringlate.settings;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.Comparator;

import io.github.lonamiwebs.stringlate.classes.resources.tags.ResTag;

import static io.github.lonamiwebs.stringlate.utilities.Constants.GITHUB_WANTED_SCOPES;

public class AppSettings extends Settings {

    public static final int SORT_ALPHABETICALLY = 0;
    public static final int SORT_STRING_LENGTH = 1;

    private static final String KEY_DOWNLOAD_ICONS = "download_icons";
    private static final String KEY_GITHUB_TOKEN = "github_access_token";
    private static final String KEY_GITHUB_SCOPE = "github_access_scope";
    private static final String KEY_STRING_SORTING = "string_sorting";

    private static final Boolean DEFAULT_DOWNLOAD_ICONS = false;
    private static final String DEFAULT_GITHUB_TOKEN = "";
    private static final String DEFAULT_GITHUB_SCOPE = "";
    private static final int DEFAULT_STRING_SORTING = SORT_ALPHABETICALLY;

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

    public Comparator<ResTag> getStringsComparator() {
        switch (getSettings().getInt(KEY_STRING_SORTING, DEFAULT_STRING_SORTING)) {
            default:
            case SORT_ALPHABETICALLY:
                return new Comparator<ResTag>() {
                    @Override
                    public int compare(ResTag o1, ResTag o2) {
                        return o1.compareTo(o2);
                    }
                };
            case SORT_STRING_LENGTH:
                return new Comparator<ResTag>() {
                    @Override
                    public int compare(ResTag o1, ResTag o2) {
                        int x = o1.getContentLength();
                        int y = o2.getContentLength();
                        return (x < y) ? -1 : ((x == y) ? 0 : 1);
                    }
                };
        }
    }

    //endregion

    //region Setters

    public void setDownloadIconsAllowed(final boolean download) {
        editSettings().putBoolean(KEY_DOWNLOAD_ICONS, download).apply();
    }

    public void setGitHubAccess(String token, String scope) {
        editSettings()
                .putString(KEY_GITHUB_TOKEN, token)
                .putString(KEY_GITHUB_SCOPE, scope).apply();
    }

    public void setStringSortMode(int mode) {
        editSettings().putInt(KEY_STRING_SORTING, mode).apply();
    }

    //endregion
}
