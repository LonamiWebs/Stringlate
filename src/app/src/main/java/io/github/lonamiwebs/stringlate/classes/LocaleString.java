package io.github.lonamiwebs.stringlate.classes;

import android.support.annotation.NonNull;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocaleString {

    final private String mLocale;
    final private String mLocaleDisplay;

    // Support a locale with any separator (i.e. "zh", "zh-TW", "zh_rTW", "zh/TW")
    final static Pattern SANITIZE_PATTERN = Pattern.compile(
            "^(\\w{2})(?:[ -_/]r?(\\w{2}))?$", Pattern.CASE_INSENSITIVE);

    public LocaleString(@NonNull String locale) {
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

    public static String getDisplay(@NonNull String locale) {
        // If there is an hyphen, then a country was also specified
        // Not sure why but the 'r' prefix is needed for the country
        if (locale.contains("-")) {
            for (Locale l : Locale.getAvailableLocales())
                if (!l.getCountry().isEmpty())
                    if (locale.equals(l.getLanguage()+"-r"+l.getCountry()))
                        return l.getDisplayName();
        } else {
            for (Locale l : Locale.getAvailableLocales())
                if (locale.equals(l.getLanguage()))
                    if (l.getCountry().isEmpty()) // Ensure that it's empty (issue #58)
                        return l.getDisplayName();
        }
        return locale;
    }

    public static boolean isValid(@NonNull String locale) {
        if (locale.contains("-")) {
            for (Locale l : Locale.getAvailableLocales())
                if (!l.getCountry().isEmpty())
                    if (locale.equals(l.getLanguage()+"-r"+l.getCountry()))
                        return true;
        } else {
            for (Locale l : Locale.getAvailableLocales())
                if (locale.equals(l.getLanguage()))
                    return true;
        }
        return false;
    }

    @NonNull
    public static String sanitizeLocale(String locale) {
        locale = locale.trim();
        Matcher m = SANITIZE_PATTERN.matcher(locale);
        if (m.find()) {
            String lang = m.group(1).toLowerCase();
            String country = m.group(2);
            if (country != null)
                return String.format("%s-r%s", lang, country.toUpperCase());
            else
                return lang;
        }
        return locale;
    }
}
