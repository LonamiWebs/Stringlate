package io.github.lonamiwebs.stringlate.interfaces;

import java.util.Comparator;

import io.github.lonamiwebs.stringlate.classes.resources.tags.ResTag;

@SuppressWarnings("UnnecessaryInterfaceModifier")
public interface SlAppSettings {
    boolean isDownloadIconsAllowed();

    String getGitHubToken();

    boolean hasGitHubAuthorization();

    void setDownloadIconsAllowed(final boolean download);

    void setGitHubAccess(String token, String scope);

    void setStringSortMode(int mode);

    String getLanguage();

    public int getStringSortMode();
}
