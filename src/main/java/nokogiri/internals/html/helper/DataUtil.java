package nokogiri.internals.html.helper;

import java.nio.charset.Charset;

public final class DataUtil {
    // Don't use StandardCharsets, as those only appear in Android API 19, and we target 10.
    public static final Charset UTF_8 = Charset.forName("UTF-8");

    private DataUtil() {}
}
