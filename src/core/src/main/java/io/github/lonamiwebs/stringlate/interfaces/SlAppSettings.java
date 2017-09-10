package io.github.lonamiwebs.stringlate.interfaces;

import io.github.lonamiwebs.stringlate.classes.resources.ResourceStringComparator;

public interface SlAppSettings {
    int DEFAULT_STRING_SORTING = ResourceStringComparator.SORT_ALPHABETICALLY;
    String DEFAULT_GITHUB_TOKEN = "";
    String DEFAULT_GITHUB_SCOPE = "";
    boolean DEFAULT_DOWNLOAD_ICONS = false;

    boolean isDownloadIconsAllowed();

    String getGitHubToken();

    boolean hasGitHubAuthorization();

    void setDownloadIconsAllowed(final boolean value);

    void setGitHubAccess(String token, String scope);

    void setStringSortMode(int value);

    String getLanguage();

    int getStringSortMode();

    String[] getGitHubScopes();
}
