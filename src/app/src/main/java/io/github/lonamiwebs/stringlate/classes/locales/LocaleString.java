package io.github.lonamiwebs.stringlate.classes.locales;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocaleString {

    final private String mLocale;
    final private String mLocaleDisplay;

    // Support a locale with any separator (i.e. "zh", "zh-TW", "zh_rTW", "zh/TW")
    private final static Pattern SANITIZE_PATTERN = Pattern.compile(
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
                    if (locale.equals(l.getLanguage() + "-r" + l.getCountry()))
                        return l.getDisplayName();
        } else {
            for (Locale l : Locale.getAvailableLocales())
                if (locale.equals(l.getLanguage()))
                    if (l.getCountry().isEmpty()) // Ensure that it's empty (issue #58)
                        return l.getDisplayName();
        }
        return locale;
    }

    // Useful when exporting to issue for example
    public static String getEnglishDisplay(@NonNull String locale) {
        if (locale.contains("-")) {
            for (Locale l : Locale.getAvailableLocales())
                if (!l.getCountry().isEmpty())
                    if (locale.equals(l.getLanguage() + "-r" + l.getCountry()))
                        return l.getDisplayName(Locale.ENGLISH);
        } else {
            for (Locale l : Locale.getAvailableLocales())
                if (locale.equals(l.getLanguage()))
                    if (l.getCountry().isEmpty())
                        return l.getDisplayName(Locale.ENGLISH);
        }
        return locale;
    }

    public static String getFullCode(@NonNull Locale locale) {
        String language = locale.getLanguage();
        String country = locale.getCountry();
        return country.isEmpty() ? language : language + "-r" + country;
    }

    public static boolean isValid(@NonNull String locale) {
        if (locale.contains("-")) {
            final String[] parts = locale.split("-");
            return isValid(new Locale(parts[0], parts[1].substring(1)));
        }

        return isValid(new Locale(locale));
    }

    private static boolean isValid(final Locale locale) {
        try {
            return locale.getISO3Language() != null && locale.getISO3Country() != null;
        } catch (MissingResourceException ignored) {
            return false;
        }
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

    @NonNull
    public static ArrayList<Locale> getCountriesForLocale(final String localeCode) {
        final ArrayList<Locale> result = new ArrayList<>();
        for (Locale l : Locale.getAvailableLocales())
            if (l.getLanguage().equals(localeCode))
                result.add(l);

        if (result.isEmpty()) {
            final Locale locale = new Locale(localeCode);
            if (isValid(locale))
                result.add(locale);
        }

        return result;
    }

    public static String getEmojiFlag(final Locale locale) {
        final String countryCode = locale.getLanguage().toUpperCase();
        return joinAsRIS(countryCode, countryCode.length() != 2);
    }

    // https://en.wikipedia.org/wiki/Regional_Indicator_Symbol
    private static String joinAsRIS(final String text, boolean addInvisibleChar) {
        try {
            final StringBuilder result = new StringBuilder();
            for (int i = 0; i != text.length(); ++i) {
                result.append(Character.toChars(Character.codePointAt(text, i) - 'A' + 0x1F1E6));
                if (addInvisibleChar && i != text.length() - 1)
                    result.append('\u2063'); // http://www.fileformat.info/info/unicode/char/2063/index.htm
            }
            return result.toString();
        } catch (IndexOutOfBoundsException ignored) {
            return "";
        }
    }
}
