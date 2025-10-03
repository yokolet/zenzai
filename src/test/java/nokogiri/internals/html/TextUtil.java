package nokogiri.internals.html;

import java.util.regex.Pattern;

public class TextUtil {
    static Pattern stripper = Pattern.compile("\\r?\\n\\s*");

    public static String stripNewlines(String text) {
        return stripper.matcher(text).replaceAll("");
    }
}