package zenzai.internal;

import org.jspecify.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.regex.Pattern;

import zenzai.helper.Validate;

/**
 A minimal String utility class. Designed for <b>internal</b> jsoup use only - the API and outcome may change without
 notice.
 */
public final class StringUtil {
    /**
     * Tests if a string is blank: null, empty, or only whitespace (" ", \r\n, \t, etc)
     * @param string string to test
     * @return if string is blank
     */
    public static boolean isBlank(@Nullable String string) {
        if (string == null || string.isEmpty())
            return true;

        int l = string.length();
        for (int i = 0; i < l; i++) {
            if (!StringUtil.isWhitespace(string.codePointAt(i)))
                return false;
        }
        return true;
    }

    /**
     * Tests if a code point is "whitespace" as defined in the HTML spec. Used for output HTML.
     * @param c code point to test
     * @return true if code point is whitespace, false otherwise
     * @see #isActuallyWhitespace(int)
     */
    public static boolean isWhitespace(int c){
        return c == ' ' || c == '\t' || c == '\n' || c == '\f' || c == '\r';
    }

    /**
     * Tests if a code point is "whitespace" as defined by what it looks like. Used for Element.text etc.
     * @param c code point to test
     * @return true if code point is whitespace, false otherwise
     */
    public static boolean isActuallyWhitespace(int c){
        return c == ' ' || c == '\t' || c == '\n' || c == '\f' || c == '\r' || c == 160;
        // 160 is &nbsp; (non-breaking space). Not in the spec but expected.
    }

    public static boolean inSorted(String needle, String[] haystack) {
        return Arrays.binarySearch(haystack, needle) >= 0;
    }

    /**
     * Create a new absolute URL, from a provided existing absolute URL and a relative URL component.
     * @param base the existing absolute base URL
     * @param relUrl the relative URL to resolve. (If it's already absolute, it will be returned)
     * @return the resolved absolute URL
     * @throws MalformedURLException if an error occurred generating the URL
     */
    public static URL resolve(URL base, String relUrl) throws MalformedURLException {
        relUrl = stripControlChars(relUrl);
        // workaround: java resolves '//path/file + ?foo' to '//path/?foo', not '//path/file?foo' as desired
        if (relUrl.startsWith("?"))
            relUrl = base.getPath() + relUrl;
        // workaround: //example.com + ./foo = //example.com/./foo, not //example.com/foo
        URL url = new URL(base, relUrl);
        String fixedFile = extraDotSegmentsPattern.matcher(url.getFile()).replaceFirst("/");
        if (url.getRef() != null) {
            fixedFile = fixedFile + "#" + url.getRef();
        }
        return new URL(url.getProtocol(), url.getHost(), url.getPort(), fixedFile);
    }

    /**
     * Create a new absolute URL, from a provided existing absolute URL and a relative URL component.
     * @param baseUrl the existing absolute base URL
     * @param relUrl the relative URL to resolve. (If it's already absolute, it will be returned)
     * @return an absolute URL if one was able to be generated, or the empty string if not
     */
    public static String resolve(String baseUrl, String relUrl) {
        // workaround: java will allow control chars in a path URL and may treat as relative, but Chrome / Firefox will strip and may see as a scheme. Normalize to browser's view.
        baseUrl = stripControlChars(baseUrl); relUrl = stripControlChars(relUrl);
        try {
            URL base;
            try {
                base = new URL(baseUrl);
            } catch (MalformedURLException e) {
                // the base is unsuitable, but the attribute/rel may be abs on its own, so try that
                URL abs = new URL(relUrl);
                return abs.toExternalForm();
            }
            return resolve(base, relUrl).toExternalForm();
        } catch (MalformedURLException e) {
            // it may still be valid, just that Java doesn't have a registered stream handler for it, e.g. tel
            // we test here vs at start to normalize supported URLs (e.g. HTTP -> http)
            return validUriScheme.matcher(relUrl).find() ? relUrl : "";
        }
    }

    public static boolean in(final String needle, final String... haystack) {
        final int len = haystack.length;
        for (int i = 0; i < len; i++) {
            if (haystack[i].equals(needle))
                return true;
        }
        return false;
    }

    public static boolean isAsciiLetter(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    public static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    public static boolean isHexDigit(char c) {
        return isDigit(c) || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
    }

    /**
     * Maintains cached StringBuilders in a flyweight pattern, to minimize new StringBuilder GCs. The StringBuilder is
     * prevented from growing too large.
     * <p>
     * Care must be taken to release the builder once its work has been completed, with {@link #releaseBuilder}
     * @return an empty StringBuilder
     */
    public static StringBuilder borrowBuilder() {
        return BuilderPool.borrow();
    }

    /**
     * Release a borrowed builder. Care must be taken not to use the builder after it has been returned, as its
     * contents may be changed by this method, or by a concurrent thread.
     * @param sb the StringBuilder to release.
     * @return the string value of the released String Builder (as an incentive to release it!).
     */
    public static String releaseBuilder(StringBuilder sb) {
        Validate.notNull(sb);
        String string = sb.toString();
        releaseBuilderVoid(sb);
        return string;
    }

    /**
     Releases a borrowed builder, but does not call .toString() on it. Useful in case you already have that string.
     @param sb the StringBuilder to release.
     @see #releaseBuilder(StringBuilder)
     */
    public static void releaseBuilderVoid(StringBuilder sb) {
        // if it hasn't grown too big, reset it and return it to the pool:
        if (sb.length() <= MaxBuilderSize) {
            sb.delete(0, sb.length()); // make sure it's emptied on release
            BuilderPool.release(sb);
        }
    }

    /**
     * Normalise the whitespace within this string; multiple spaces collapse to a single, and all whitespace characters
     * (e.g. newline, tab) convert to a simple space.
     * @param string content to normalise
     * @return normalised string
     */
    public static String normaliseWhitespace(String string) {
        StringBuilder sb = StringUtil.borrowBuilder();
        appendNormalisedWhitespace(sb, string, false);
        return StringUtil.releaseBuilder(sb);
    }


    /**
     * After normalizing the whitespace within a string, appends it to a string builder.
     * @param accum builder to append to
     * @param string string to normalize whitespace within
     * @param stripLeading set to true if you wish to remove any leading whitespace
     */
    public static void appendNormalisedWhitespace(StringBuilder accum, String string, boolean stripLeading) {
        boolean lastWasWhite = false;
        boolean reachedNonWhite = false;

        int len = string.length();
        int c;
        for (int i = 0; i < len; i+= Character.charCount(c)) {
            c = string.codePointAt(i);
            if (isActuallyWhitespace(c)) {
                if ((stripLeading && !reachedNonWhite) || lastWasWhite)
                    continue;
                accum.append(' ');
                lastWasWhite = true;
            }
            else if (!isInvisibleChar(c)) {
                accum.appendCodePoint(c);
                lastWasWhite = false;
                reachedNonWhite = true;
            }
        }
    }

    public static boolean isInvisibleChar(int c) {
        return c == 8203 || c == 173; // zero width sp, soft hyphen
        // previously also included zw non join, zw join - but removing those breaks semantic meaning of text
    }

    private static final Pattern validUriScheme = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+-.]*:");
    private static final Pattern controlChars = Pattern.compile("[\\x00-\\x1f]*"); // matches ascii 0 - 31, to strip from url
    private static final Pattern extraDotSegmentsPattern = Pattern.compile("^/(?>(?>\\.\\.?/)+)");
    private static final int MaxBuilderSize = 8 * 1024;
    private static final int InitBuilderSize = 1024;
    private static final SoftPool<StringBuilder> BuilderPool = new SoftPool<>(
            () -> new StringBuilder(InitBuilderSize));

    private static String stripControlChars(final String input) {
        return controlChars.matcher(input).replaceAll("");
    }
}
