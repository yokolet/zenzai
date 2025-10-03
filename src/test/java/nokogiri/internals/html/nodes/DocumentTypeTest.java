package nokogiri.internals.html.nodes;

import org.junit.jupiter.api.Test;
import nokogiri.internals.html.parser.Parser;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentTypeTest {
    @Test
    public void testConstructorWithBlankName() {
        DocumentType docType = new DocumentType("", "", "");
        assertNotNull(docType);
    }

    @Test
    public void testConstructorWithNulls() {
        assertThrows(IllegalArgumentException.class, () -> new DocumentType("html", null, null));
    }

    @Test
    public void testConstructorWithBlankPublicAndSystem() {
        DocumentType docType = new DocumentType("html", "", "");
        assertNotNull(docType);
    }

    @Test
    public void testOuterHtml() {
        DocumentType docType = new DocumentType("html", "", "");
        assertEquals("<!doctype html>", docType.outerHtml());

        DocumentType docType2 = new DocumentType("html", "-//IETF//DTD HTML//", "");
        assertEquals("<!DOCTYPE html PUBLIC \"-//IETF//DTD HTML//\">", docType2.outerHtml());

        DocumentType docType3 = new DocumentType("html", "", "http://www.ibm.com/data/dtd/v11/ibmxhtml1-transitional.dtd");
        assertEquals("<!DOCTYPE html SYSTEM \"http://www.ibm.com/data/dtd/v11/ibmxhtml1-transitional.dtd\">", docType3.outerHtml());

        DocumentType docType4 = new DocumentType("notHtml", "--public", "--system");
        assertEquals("<!DOCTYPE notHtml PUBLIC \"--public\" \"--system\">", docType4.outerHtml());
    }

    @Test
    public void testRoundTrip() {
        String html = "<!DOCTYPE html>";
        assertEquals("<!doctype html>", htmlOutput(html));

        html = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">";
        assertEquals(html, htmlOutput(html));

        html = "<!DOCTYPE html SYSTEM \"exampledtdfile.dtd\">";
        assertEquals(html, htmlOutput(html));

        html = "<!DOCTYPE html SYSTEM \"about:legacy-compat\">";
        assertEquals(html, htmlOutput(html));
    }

    @Test
    public void testAttributes() {
        Document doc = Parser.parse("<!DOCTYPE html>", "");
        DocumentType docType = doc.documentType();
        assertEquals("#doctype", docType.nodeName());
        assertEquals("html", docType.name());
        assertEquals("html", docType.attr("name"));
        assertEquals("", docType.publicId());
        assertEquals("", docType.systemId());
    }

    private String htmlOutput(String html) {
        Document doc = Parser.parse(html, "");
        DocumentType docType = (DocumentType) doc.childNode(0);
        return docType.outerHtml();
    }
}
