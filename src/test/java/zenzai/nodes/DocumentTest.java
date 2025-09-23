package zenzai.nodes;

import org.junit.jupiter.api.Test;
import zenzai.TextUtil;
import zenzai.parser.ParseSettings;
import zenzai.parser.Parser;
import zenzai.parser.Tag;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentTest {
    private static final String charsetUtf8 = "UTF-8";
    private static final String charsetIso8859 = "ISO-8859-1";

    @Test
    public void setTextPreservesDocumentStructure() {
        Document doc = Parser.parse("<p>Hello</p>", "");
        doc.text("Replaced");
        assertEquals("Replaced", doc.text());
        assertEquals("Replaced", doc.body().text());
        assertEquals(1, doc.select("head").size());
    }

    @Test
    public void testTitles() {
        Document noTitle = Parser.parse("<p>Hello</p>", "");
        Document withTitle = Parser.parse("<title>First</title><title>Ignore</title><p>Hello</p>", "");
        assertEquals("", noTitle.title());
        noTitle.title("Hello");
        assertEquals("Hello", noTitle.title());
        assertEquals("Hello", noTitle.select("title").first().text());

        assertEquals("First", withTitle.title());
        withTitle.title("Hello");
        assertEquals("Hello", withTitle.title());
        assertEquals("Hello", withTitle.select("title").first().text());
    }

    @Test
    public void testOutputEncoding() {
        Document doc = Parser.parse("<p title=π>π & < > </p>", "");
        // default is utf-8
        assertEquals("<p title=\"π\">π &amp; &lt; &gt;</p>", doc.body().html());
        assertEquals("UTF-8", doc.outputSettings().charset().name());

        doc.outputSettings().charset("ascii");
        assertEquals(Entities.EscapeMode.base, doc.outputSettings().escapeMode());
        assertEquals("<p title=\"&#x3c0;\">&#x3c0; &amp; &lt; &gt;</p>", doc.body().html());

        doc.outputSettings().escapeMode(Entities.EscapeMode.extended);
        assertEquals("<p title=\"&pi;\">&pi; &amp; &lt; &gt;</p>", doc.body().html());
    }

    @Test
    public void testXmlReference() {
        Document doc = Parser.parse("&lt; &gt; &amp; &quot; &apos; &times;", "");
        doc.outputSettings().escapeMode(Entities.EscapeMode.xhtml);
        assertEquals("&lt; &gt; &amp; \" ' ×", doc.body().html());
    }

    @Test
    public void testNormalizesStructure() {
        String html = "<html><head><script>one</script><noscript><p>two</p></noscript></head><body><p>three</p></body><p>four</p></html>";
        String expected = "<html><head><script>one</script><noscript>&lt;p&gt;two</noscript></head><body><p>three</p><p>four</p></body></html>";
        Document doc = Parser.parse(html, "");
        assertEquals(expected, TextUtil.stripNewlines(doc.html()));
    }

    @Test
    public void accessorsWillNormalizeStructure() {
        Document doc = Parser.parse("", "");
        //assertEquals("", doc.html()); // doc.html() returns a normalized structure

        Element body = doc.body();
        assertEquals("body", body.tagName());
        Element head = doc.head();
        assertEquals("head", head.tagName());
        assertEquals("<html><head></head><body></body></html>", TextUtil.stripNewlines(doc.html()));
    }

    @Test
    public void accessorsAreCaseInsensitive() {
        String html = "<!DOCTYPE html><HTML><HEAD><TITLE>SHOUTY</TITLE></HEAD><BODY>HELLO</BODY></HTML>";
        Parser parser = Parser.htmlParser().settings(ParseSettings.preserveCase);
        Document doc = parser.parseInput(html, "");
        Element body = doc.body();
        assertEquals("BODY", body.tagName());
        assertEquals("body", body.normalName());
        Element head = doc.head();
        assertEquals("HEAD", head.tagName());
        assertEquals("head", head.normalName());

        Element root = doc.selectFirst("html");
        assertEquals("HTML", root.tagName());
        assertEquals("html", root.normalName());
        assertEquals("SHOUTY", doc.title());
    }

    @Test
    public void testClone() {
        Document doc = Parser.parse("<title>Hello</title> <p>One<p>Two", "");
        Document clone = doc.clone();
        assertNotSame(doc, clone);
        assertTrue(doc.hasSameValue(clone));
        assertSame(doc.parser(), clone.parser());
        assertNotSame(doc.outputSettings(), clone.outputSettings());

        String expected = "<html><head><title>Hello</title></head><body><p>One</p><p>Two</p></body></html>";
        assertEquals(expected, TextUtil.stripNewlines(clone.html()));

        clone.title("Hello there");
        assertFalse(doc.hasSameValue(clone));
        clone.expectFirst("p").text("One more").attr("id", "1");
        String expected2 = "<html><head><title>Hello there</title></head><body><p id=\"1\">One more</p><p>Two</p></body></html>";
        assertEquals(expected2, TextUtil.stripNewlines(clone.html()));
        String expected3 = "<html><head><title>Hello</title></head><body><p>One</p><p>Two</p></body></html>";
        assertEquals(expected3, TextUtil.stripNewlines(doc.html()));
    }

    @Test
    public void testBasicIndent() {
        Document doc = Parser.parse("<title>Hello</title> <p>One\n<p>Two\n", "");
        String expected = "<html>\n <head>\n  <title>Hello</title>\n </head>\n <body>\n  <p>One</p>\n  <p>Two</p>\n </body>\n</html>";
        assertEquals(expected, doc.html());
    }

    @Test
    public void testClonedDeclaration() {
        Document doc = Parser.parse("<!DOCTYPE html><html><head><title>Doctype test", "");
        Document clone = doc.clone();
        assertEquals(doc.html(), clone.html());
        assertEquals("<!doctype html><html><head><title>Doctype test</title></head><body></body></html>",
                TextUtil.stripNewlines(clone.html()));
    }

//    @Test public void testLocation() throws IOException {
//        // tests location vs base href
//        File in = ParseTest.getFile("/htmltests/basehref.html");
//        Document doc = Jsoup.parse(in, "UTF-8", "http://example.com/");
//        String location = doc.location();
//        String baseUri = doc.baseUri();
//        assertEquals("http://example.com/", location);
//        assertEquals("https://example.com/path/file.html?query", baseUri);
//        assertEquals("./anotherfile.html", doc.expectFirst("a").attr("href"));
//        assertEquals("https://example.com/path/anotherfile.html", doc.expectFirst("a").attr("abs:href"));
//    }

    @Test
    public void testLocationFromString() {
        Document doc = Parser.parse("<p>Hello", "");
        assertEquals("", doc.location());
    }

    @Test
    public void testHtmlAndXmlSyntax() {
        String html = "<!DOCTYPE html><body><img async checked='checked' src='&<>\"'>&lt;&gt;&amp;&quot;<foo />bar";
        Parser parser = Parser.htmlParser();
        parser.tagSet().valueOf("foo", Parser.NamespaceXml).set(Tag.SelfClose);
        Document doc = parser.parseInput(html, "");

        doc.outputSettings().syntax(Document.OutputSettings.Syntax.html);
//        assertEquals("<!doctype html>\n" +
//                "<html>\n" +
//                " <head></head>\n" +
//                " <body>\n" +
//                "  <img async checked src=\"&amp;&lt;&gt;&quot;\">&lt;&gt;&amp;\"<foo></foo>bar\n" + // html won't include self-closing
//                " </body>\n" +
//                "</html>", doc.html());

        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
//        assertEquals("<!DOCTYPE html>\n" +
//                "<html>\n" +
//                " <head></head>\n" +
//                " <body>\n" +
//                "  <img async=\"\" checked=\"checked\" src=\"&amp;&lt;&gt;&quot;\" />&lt;&gt;&amp;\"<foo />bar\n" + // xml will
//                " </body>\n" +
//                "</html>", doc.html());
    }
}
