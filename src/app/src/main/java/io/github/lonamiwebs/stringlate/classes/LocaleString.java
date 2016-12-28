package io.github.lonamiwebs.stringlate.classes;

import java.util.Locale;

public class LocaleString {

    final String mLocale;
    final String mLocaleDisplay;

    public LocaleString(String locale) {
        mLocale = locale;
        mLocaleDisplay = getDisplay(locale);
    }

    public String getCode() {
        return mLocale;
    }

    @Override
    public String toString() {
        return mLocaleDisplay;
    }

    public static String getDisplay(String locale) {
        if (locale.contains("-")) {
            // If there is an hyphen, then a country was also specified
            for (Locale l : Locale.getAvailableLocales())
                if (!l.getCountry().isEmpty())
                    if (locale.equals(l.getLanguage()+"-"+l.getCountry()))
                        return l.getDisplayName();
        } else {
            for (Locale l : Locale.getAvailableLocales())
                if (locale.equals(l.getLanguage()))
                    return l.getDisplayName();
        }
        return locale;
    }

    public static boolean isValid(String locale) {
        if (locale.contains("-")) {
            // If there is an hyphen, then a country was also specified
            for (Locale l : Locale.getAvailableLocales())
                if (!l.getCountry().isEmpty())
                    if (locale.equals(l.getLanguage()+"-"+l.getCountry()))
                        return true;
        } else {
            for (Locale l : Locale.getAvailableLocales())
                if (locale.equals(l.getLanguage()))
                    return true;
        }
        return false;
    }
}
