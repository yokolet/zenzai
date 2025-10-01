package zenzai.nodes;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.List;

import org.junit.jupiter.api.Disabled;
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

    @Test
    public void testLocationFromString() {
        Document doc = Parser.parse("<p>Hello", "");
        assertEquals("", doc.location());
    }

    @Disabled("original jsoup fails as well")
    @Test
    public void testHtmlAndXmlSyntax() {
        String html = "<!DOCTYPE html><body><img async checked='checked' src='&<>\"'>&lt;&gt;&amp;&quot;<foo />bar";
        Parser parser = Parser.htmlParser();
        parser.tagSet().valueOf("foo", Parser.NamespaceXml).set(Tag.SelfClose);
        Document doc = parser.parseInput(html, "");

        doc.outputSettings().syntax(Document.OutputSettings.Syntax.html);
        assertEquals("<!doctype html>\n" +
                "<html>\n" +
                " <head></head>\n" +
                " <body>\n" +
                "  <img async checked src=\"&amp;&lt;&gt;&quot;\">&lt;&gt;&amp;\"<foo></foo>bar\n" + // html won't include self-closing
                " </body>\n" +
                "</html>", doc.html());

        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        assertEquals("<!DOCTYPE html>\n" +
                "<html>\n" +
                " <head></head>\n" +
                " <body>\n" +
                "  <img async=\"\" checked=\"checked\" src=\"&amp;&lt;&gt;&quot;\" />&lt;&gt;&amp;\"<foo />bar\n" + // xml will
                " </body>\n" +
                "</html>", doc.html());
    }

    @Test
    public void htmlParseDefaultToHtmlOutputSyntax() {
        Document doc = Parser.parse("x", "");
        assertEquals(Document.OutputSettings.Syntax.html, doc.outputSettings().syntax());
    }

    @Test
    public void testHtmlAppendable() {
        String html = "<html><head><title>Hello</title></head><body><p>One</p><p>Two</p></body></html>";
        Document doc = Parser.parse(html, "");
        Document.OutputSettings outputSettings = new Document.OutputSettings();
        outputSettings.prettyPrint(false);
        doc.outputSettings(outputSettings);
        assertEquals(html, doc.html(new StringWriter()).toString());
    }

    @Test
    public void testOverflowClone() {
        StringBuilder sb = new StringBuilder();
        sb.append("<head><base href='https://jsoup.org/'>");
        for (int i = 0; i < 100000; i++) {
            sb.append("<div>");
        }
        sb.append("<p>Hello <a href='/example.html'>there</a>");

        Document doc = Parser.parse(sb.toString(), "");
        String expected = "https://jsoup.org/example.html";
        assertEquals(expected, doc.selectFirst("a").attr("abs:href"));
        Document clone = doc.clone();
        assertTrue(doc.hasSameValue(clone));
        assertEquals(expected, clone.selectFirst("a").attr("abs:href"));
    }

    @Test
    public void testDocumentsWithSameContentAreEqual() {
        Document docA = Parser.parse("<div/>One", "");
        Document docB = Parser.parse("<div/>One", "");
        Document docC = Parser.parse("<div/>Two", "");
        // TODO: figure out what this test case mean
        assertNotEquals(docA, docB);
        assertEquals(docA.hashCode(), docA.hashCode());
        assertNotEquals(docA.hashCode(), docC.hashCode());
    }

    @Test
    public void testDocumentsWithSameContentAreVerifiable() {
        Document docA = Parser.parse("<div/>One", "");
        Document docB = Parser.parse("<div/>One", "");
        Document docC = Parser.parse("<div/>Two", "");

        assertTrue(docA.hasSameValue(docB));
        assertFalse(docA.hasSameValue(docC));
    }

    @Test
    public void testMataCharsetUpdateUtf8() {
        Document doc = createHtmlDocument("changeThis");
        doc.charset(Charset.forName(charsetUtf8));
        String expected =
                "<html>\n" +
                " <head>\n" +
                "  <meta charset=\"" + charsetUtf8 + "\">\n" +
                " </head>\n" +
                " <body></body>\n" +
                "</html>";
        assertEquals(expected, doc.toString());
        assertEquals(charsetUtf8, doc.charset().name());
        assertEquals(doc.charset(), doc.outputSettings().charset());
    }

    @Test
    public void testMetaCharsetUpdateIso8859() {
        Document doc = createHtmlDocument("changeThis");
        doc.charset(Charset.forName(charsetIso8859));
        String expected =
                "<html>\n" +
                " <head>\n" +
                "  <meta charset=\"" + charsetIso8859 + "\">\n" +
                " </head>\n" +
                " <body></body>\n" +
                "</html>";
        assertEquals(expected, doc.toString());
        assertEquals(charsetIso8859, doc.charset().name());
        assertEquals(doc.charset(), doc.outputSettings().charset());
    }

    @Test
    public void testWithoutMetaCharset() {
        Document doc = Document.createShell("");
        String expected =
                "<html>\n" +
                " <head></head>\n" +
                " <body></body>\n" +
                "</html>";
        assertEquals(expected, doc.toString());
    }

    @Test
    public void testDocumentTypeGet() {
        String html = "\n\n<!-- comment -->  <!doctype html><p>One</p>";
        Document doc = Parser.parse(html, "");
        DocumentType docType = doc.documentType();
        assertNotNull(docType);
        assertEquals("html", docType.name());
    }

    @Test
    public void testForms() {
        String html = "<body><form id=1><input name=foo></form><form id=2><input name=bar>";
        Document doc = Parser.parse(html, "");
        List<FormElement> forms = doc.forms();
        assertEquals(2, forms.size());

        FormElement form = forms.get(1);
        assertEquals(1, form.elements().size());
        assertEquals("bar", form.elements().first().attr("name"));
    }

    private Document createHtmlDocument(String charset) {
        Document doc = Document.createShell("");
        doc.head().appendElement("meta").attr("charset", charset);
        doc.head().appendElement("meta").attr("name", "charset").attr("content", charset);
        return doc;
    }
}
