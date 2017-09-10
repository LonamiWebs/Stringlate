package io.github.lonamiwebs.stringlate.cli;

import io.github.lonamiwebs.stringlate.classes.resources.ResourceStringComparator;
import io.github.lonamiwebs.stringlate.settings.AppSettings;

public class Main {

    public static void main(String[] args) {
        AppSettings appSettings = new AppSettings();
        System.out.println(appSettings.getPathToPropertiesFile());

        appSettings.setStringSortMode(ResourceStringComparator.SORT_STRING_LENGTH);
        appSettings.setDownloadIconsAllowed(false);
        System.out.println("Icons allowed: " + appSettings.isDownloadIconsAllowed());
        appSettings.setDownloadIconsAllowed(true);
        System.out.println("Icons allowed: " + appSettings.isDownloadIconsAllowed());
    }
}
