package nokogiri.internals.html.internal;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import nokogiri.internals.html.nodes.Attribute;
import nokogiri.internals.html.nodes.Document;

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

    /** Minimal helper to get an otherwise OK HTML name like "foo&lt;bar" to "foo_bar". */
    @Nullable
    public static String xmlSafeTagName(final String tagname) {
        return Attribute.getValidKey(tagname, Document.OutputSettings.Syntax.xml); // Reuses the Attribute key normal, which is same for xml tag names
    }
}
