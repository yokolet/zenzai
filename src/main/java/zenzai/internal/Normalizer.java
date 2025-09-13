package zenzai.internal;

import java.util.Locale;

public final class Normalizer {
    /** Drops the input string to lower case. */
    public static String lowerCase(final String input) {
        return input != null ? input.toLowerCase(Locale.ROOT) : "";
    }

    /** Lower-cases and trims the input string. */
    public static String normalize(final String input) {
        return lowerCase(input).trim();
    }

    /**
     If a string literal, just lower case the string; otherwise lower-case and trim.
     @deprecated internal function; will be removed in a future version.
     */
    @Deprecated
    public static String normalize(final String input, boolean isStringLiteral) {
        return isStringLiteral ? lowerCase(input) : normalize(input);
    }
}
