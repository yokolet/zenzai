package zenzai.comparison;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestBasic {
    @Test
    public void testCreateDocumentFromString() {
        String html = "<html><head><title>First parse</title></head>"
                + "<body><p>Parsed HTML into a doc.</p></body></html>";
        Document doc = Jsoup.parse(html);
        assertEquals(doc.title(), "First parse");
    }
}
