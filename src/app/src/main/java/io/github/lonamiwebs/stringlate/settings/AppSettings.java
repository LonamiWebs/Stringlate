package io.github.lonamiwebs.stringlate.settings;

import android.content.Context;
import android.support.annotation.NonNull;

import net.gsantner.opoc.util.AppSettingsBase;

import io.github.lonamiwebs.stringlate.R;
import io.github.lonamiwebs.stringlate.interfaces.SlAppSettings;

import static io.github.lonamiwebs.stringlate.utilities.Constants.GITHUB_WANTED_SCOPES;

public class AppSettings extends AppSettingsBase implements SlAppSettings {
    //region Constructor

    public AppSettings(final Context context) {
        super(context, null);
    }

    //endregion

    //region Getters

    public boolean isDownloadIconsAllowed() {
        return getBool(rstr(R.string.pref_key__download_icons), DEFAULT_DOWNLOAD_ICONS);
    }

    // Tokens seem to be saved OK as shared preferences http://stackoverflow.com/q/10161266
    // Another option, AccountManager: http://stackoverflow.com/q/14437096
    @NonNull
    public String getGitHubToken() {
        return getString(rstr(R.string.pref_key__github_access_token), DEFAULT_GITHUB_TOKEN);
    }

    @NonNull
    public String[] getGitHubScopes() {
        return getString(rstr(R.string.pref_key__github_access_scope), DEFAULT_GITHUB_SCOPE).split(",");
    }

    public boolean hasGitHubAuthorization() {
        return !getGitHubToken().isEmpty() &&
                getGitHubScopes().length == GITHUB_WANTED_SCOPES.length;
    }

    public int getStringSortMode() {
        return getInt(rstr(R.string.pref_key__string_sorting), DEFAULT_STRING_SORTING);
    }

    //endregion

    //region Setters

    public void setDownloadIconsAllowed(final boolean value) {
        setBool(rstr(R.string.pref_key__download_icons), value);
    }

    public void setGitHubAccess(String token, String scope) {
        setString(rstr(R.string.pref_key__github_access_token), token);
        setString(rstr(R.string.pref_key__github_access_scope), scope);
    }

    public void setStringSortMode(int value) {
        setInt(rstr(R.string.pref_key__string_sorting), value);
    }

    public String getLanguage() {
        return getString(R.string.pref_key__language, "");
    }

    //endregion
}
