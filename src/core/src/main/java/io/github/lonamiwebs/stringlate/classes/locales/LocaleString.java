package io.github.lonamiwebs.stringlate.classes.locales;

import java.util.ArrayList;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.regex.Pattern;

public class LocaleString {

    final private String mLocale;
    final private String mLocaleDisplay;

    // Support a locale with any separator (i.e. "zh", "zh-TW", "zh_rTW", "zh/TW")
    private final static Pattern SANITIZE_PATTERN = Pattern.compile(
            "^(\\w{2})(?:[ -_/]r?(\\w{2}))?$", Pattern.CASE_INSENSITIVE);

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

    public static String getDisplay(String localeCode) {
        final Locale locale = getLocale(localeCode);
        if (isValid(locale))
            return locale.getDisplayName();
        else
            return localeCode;
    }

    // Useful when exporting to issue for example
    public static String getEnglishDisplay(String localeCode) {
        final Locale locale = getLocale(localeCode);
        if (isValid(locale))
            return locale.getDisplayName(Locale.ENGLISH);
        else
            return localeCode;
    }

    public static String getFullCode(Locale locale) {
        String language = locale.getLanguage();
        String country = locale.getCountry();
        return country.isEmpty() ? language : language + "-r" + country;
    }

    public static Locale getLocale(String locale) {
        if (locale.contains("-")) {
            final String[] parts = locale.split("-");
            return new Locale(parts[0], parts[1].substring(1));
        }

        return new Locale(locale);
    }

    private static boolean isValid(final Locale locale) {
        try {
            return locale.getISO3Language() != null && locale.getISO3Country() != null;
        } catch (MissingResourceException ignored) {
            return false;
        }
    }

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
        String countryCode = locale.getCountry();
        if (countryCode.isEmpty())
            return joinAsRIS(locale.getLanguage().toUpperCase(), true);
        else
            return joinAsRIS(countryCode.toUpperCase(), countryCode.length() != 2);
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
