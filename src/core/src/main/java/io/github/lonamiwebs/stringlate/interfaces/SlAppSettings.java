package io.github.lonamiwebs.stringlate.interfaces;

import io.github.lonamiwebs.stringlate.classes.resources.ResourceStringComparator;

public interface SlAppSettings {
    int DEFAULT_STRING_SORTING = ResourceStringComparator.SORT_ALPHABETICALLY;
    String DEFAULT_GITHUB_TOKEN = "";
    String DEFAULT_GITHUB_SCOPE = "";
    boolean DEFAULT_DOWNLOAD_ICONS = false;

    // Not very secret anymore is it v ? Some discussion available at:
    // http://stackoverflow.com/q/4057277 and http://stackoverflow.com/q/4419915
    String GITHUB_CLIENT_ID = "994d17302a9e34077cd9";
    String GITHUB_CLIENT_SECRET = "863d91a38332b3648cd951c0c498fd2520a8dd9d";

    boolean isDownloadIconsAllowed();

    String getGitHubToken();

    boolean hasGitHubAuthorization();

    void setDownloadIconsAllowed(final boolean value);

    void setGitHubAccess(String token, String scope);

    void setStringSortMode(int value);

    String getLanguage();

    int getStringSortMode();

    String[] getGitHubScopes();

    String getGitHubClientSecret();

    void setGitHubClientSecret(String value);

    String getGitHubClientId();

    void setGitHubClientId(String value);

}
