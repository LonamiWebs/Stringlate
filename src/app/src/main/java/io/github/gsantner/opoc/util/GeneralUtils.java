package io.github.gsantner.opoc.util;

import java.util.regex.Pattern;

@SuppressWarnings({"WeakerAccess", "unused", "SameParameterValue", "SpellCheckingInspection", "deprecation"})
public class GeneralUtils {
    public static String toTitleCase(final String string) {
        return toTitleCase(string, Pattern.compile("\\s"));
    }

    public static String toTitleCase(final String string, final Pattern separator) {
        if (string == null)
            return null;

        final StringBuilder result = new StringBuilder();
        boolean nextTitleCase = true;

        for (char c : string.toCharArray()) {
            if (separator.matcher(String.valueOf(c)).matches()) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }

            result.append(c);
        }

        return result.toString();
    }
}
