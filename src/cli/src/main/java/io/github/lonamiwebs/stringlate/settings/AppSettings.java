package io.github.lonamiwebs.stringlate.settings;

import net.gsantner.opoc.util.AppSettingsBaseJ;

import java.util.Comparator;

import io.github.lonamiwebs.stringlate.classes.resources.tags.ResTag;
import io.github.lonamiwebs.stringlate.interfaces.SlAppSettings;

public class AppSettings extends AppSettingsBaseJ implements SlAppSettings {
    public AppSettings(Class classOfApplicationPackage) {
        super(io.github.lonamiwebs.stringlate.cli.Main.class);
    }

    @Override
    public boolean isDownloadIconsAllowed() {
        return false;
    }

    @Override
    public String getGitHubToken() {
        return null;
    }

    @Override
    public boolean hasGitHubAuthorization() {
        return false;
    }

    @Override
    public void setDownloadIconsAllowed(boolean download) {

    }

    @Override
    public void setGitHubAccess(String token, String scope) {

    }

    @Override
    public void setStringSortMode(int mode) {

    }

    @Override
    public String getLanguage() {
        return null;
    }

    @Override
    public Comparator<ResTag> getStringsComparator() {
        return null;
    }
}
