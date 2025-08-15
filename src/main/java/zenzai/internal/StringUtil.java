package zenzai.internal;

import org.jspecify.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.regex.Pattern;

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

    private static final Pattern validUriScheme = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+-.]*:");
    private static final Pattern controlChars = Pattern.compile("[\\x00-\\x1f]*"); // matches ascii 0 - 31, to strip from url
    private static final Pattern extraDotSegmentsPattern = Pattern.compile("^/(?>(?>\\.\\.?/)+)");

    private static String stripControlChars(final String input) {
        return controlChars.matcher(input).replaceAll("");
    }
}
