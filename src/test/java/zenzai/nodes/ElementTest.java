package zenzai.nodes;

import org.junit.jupiter.api.Test;
import zenzai.TextUtil;
import zenzai.parser.Parser;
import zenzai.select.Elements;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ElementTest {
    private String html = "<div id=div1><p>Hello</p><p>Another <b>element</b></p><div id=div2><img src=foo.png></div></div>";

    @Test
    public void testId() {
        Document doc = Parser.parse("<div id=Foo>", "");
        Element el = doc.selectFirst("div");
        assertEquals("Foo", el.id());
    }

    @Test
    public void testSetId() {
        Document doc = Parser.parse("<div id=Boo>", "");
        Element el = doc.selectFirst("div");
        el.id("Foo");
        assertEquals("Foo", el.id());
    }

    @Test
    public void testGetElementsByTag() {
        Document doc = Parser.parse(html, "");
        List<Element> els = doc.select("div");
        assertEquals(2, els.size());
        assertEquals("div1", els.get(0).id());
        assertEquals("div2", els.get(1).id());

        List<Element> els2 = doc.getElementsByTag("p");
        assertEquals(2, els2.size());
        assertEquals("Hello", ((TextNode)els2.get(0).childNode(0)).getWholeText());
        assertEquals("Another ", ((TextNode)els2.get(1).childNode(0)).getWholeText());
        List<Element> els3 = doc.getElementsByTag("P");
        assertEquals(els2, els3);

        List<Element> els4 = doc.getElementsByTag("img");
        assertEquals("foo.png", els4.get(0).attr("src"));

        List<Element> els5 = doc.getElementsByTag("wtf");
        assertEquals(0, els5.size());
    }


    @Test
    public void testGetText() {
        Document doc = Parser.parse(html, "");
        assertEquals("Hello Another element", doc.text());
        assertEquals("Another element", doc.getElementsByTag("p").get(1).text());
    }

    @Test
    public void testGetChildText() {
        Document doc = Parser.parse("<p>Hello <b>there</b> now", "");
        Element el = doc.getElementsByTag("p").first();
        assertEquals("Hello there now", el.text());
        assertEquals("Hello now", el.ownText());
    }

    @Test
    public void testGetWholeText() {
        Document doc = Parser.parse("<p> Hello\nthere &nbsp;  </p>", "");
        assertEquals(" Hello\nthere    ", doc.wholeText());

        doc = Parser.parse("<p>Hello  \n  there</p>", "");
        assertEquals("Hello  \n  there", doc.wholeText());

        doc = Parser.parse("<p>Hello  <div>\n  there</div></p>", "");
        assertEquals("Hello  \n  there", doc.wholeText());
    }

    @Test
    public void testGetSiblings() {
        Document doc = Parser.parse("<div><p>Hello<p id=1>there<p>this<p>is<p>an<p id=last>element</div>", "");
        Element el = doc.getElementsByTag("p").get(1);
        assertEquals("there", el.text());
        assertEquals("Hello", el.previousElementSibling().text());
        assertEquals("this", el.nextElementSibling().text());
        assertEquals("Hello", el.firstElementSibling().text());
        assertEquals("element", el.lastElementSibling().text());
        assertNull(el.lastElementSibling().nextElementSibling());
        assertNull(el.firstElementSibling().previousElementSibling());
    }

    @Test
    public void testOrphanElementSiblings() {
        Element el = new Element("p");
        assertSame(el, el.firstElementSibling());
        assertSame(el, el.lastElementSibling());
    }

    @Test
    public void testGetParents() {
        Document doc = Parser.parse("<div><p>Hello <span>there</span></div>", "");
        Element span = doc.select("span").first();
        Elements parents = span.parents();

        assertEquals(4, parents.size());
        assertEquals("p", parents.get(0).tagName());
        assertEquals("div", parents.get(1).tagName());
        assertEquals("body", parents.get(2).tagName());
        assertEquals("html", parents.get(3).tagName());

        Element el = new Element("p");
        assertEquals(0, el.parents().size());
    }

    @Test
    public void testElementSiblingIndex() {
        Document doc = Parser.parse("<div><p>One</p>...<p>Two</p>...<p>Three</p>", "");
        Elements ps = doc.getElementsByTag("p");
        assertEquals(0, ps.get(0).elementSiblingIndex());
        assertEquals(1, ps.get(1).elementSiblingIndex());
        assertEquals(2, ps.get(2).elementSiblingIndex());
    }

    @Test
    public void testSetText() {
        String html = "<div id=1>Hello <p>there <b>now</b></p></div>";
        Document doc = Parser.parse(html, "");
        assertEquals("Hello there now", doc.text());
        assertEquals("there now", doc.getElementsByTag("p").get(0).text());

        Element el = doc.getElementById("1");
        el.text("Gone");
        assertEquals("Gone", el.text());
        assertEquals(0, doc.getElementsByTag("p").size());
    }

    @Test
    public void testAddNewElements() {
        Document doc = Parser.parse("<div id=1><p>Hello</p></div>", "");
        Element div = doc.getElementById("1");
        div.appendElement("p").text("there");
        div.appendElement("P").attr("CLASS", "second").text("now");
        String expected = "<html><head></head><body><div id=\"1\"><p>Hello</p><p>there</p><p class=\"second\">now</p></div></body></html>";
        assertEquals(expected, TextUtil.stripNewlines(doc.html()));

        // test sibling reindex
        Elements els = div.getElementsByTag("p");
        for (int i = 0; i <els.size(); i++) {
            assertEquals(i, els.get(i).elementSiblingIndex());
        }
    }

    @Test
    public void testAddBooleanAttributes() {
        Element div = new Element("div");
        div.attr("true", true);
        assertTrue(div.hasAttr("true"));
        assertEquals("", div.attr("true"));

        div.attr("false", "value");
        div.attr("false", false);  // removes a boolean attribute

        List<Attribute> attributes = div.attributes().asList();
        assertEquals(1, attributes.size());
        assertFalse(div.hasAttr("false"));
        assertEquals("<div true></div>", div.outerHtml());
    }

    @Test
    public void testAppendTableRow() {
        Document doc = Parser.parse("<table><tr><td>1</td></tr></table>", "");
        Element tbody = doc.getElementsByTag("tbody").first();
        tbody.append("<tr><td>2</td></tr>");
        String expected = "<table><tbody><tr><td>1</td></tr><tr><td>2</td></tr></tbody></table>";
        assertEquals(expected, TextUtil.stripNewlines(doc.body().html()));
    }

    @Test
    public void testPrependTableRow() {
        Document doc = Parser.parse("<table><tr><td>1</td></tr></table>", "");
        Element tbody = doc.getElementsByTag("tbody").first();
        tbody.prepend("<tr><td>2</td></tr></table>");
        String expected = "<table><tbody><tr><td>2</td></tr><tr><td>1</td></tr></tbody></table>";
        assertEquals(expected, TextUtil.stripNewlines(doc.body().html()));

        // test sibling reindex
        Elements els = doc.getElementsByTag("tr");
        for (int i = 0; i < els.size(); i++) {
            assertEquals(i, els.get(i).elementSiblingIndex());
        }
    }

    @Test
    public void testPrependElement() {
        Document doc = Parser.parse("<div id=1><p>Hello</p></div>", "");
        Element div = doc.getElementById("1");
        div.prependElement("p").text("Before");
        String expected = "<div id=\"1\"><p>Before</p><p>Hello</p></div>";
        assertEquals(expected, TextUtil.stripNewlines(doc.body().html()));
        assertEquals("Before", div.child(0).text());
        assertEquals("Hello", div.child(1).text());
    }

    @Test
    public void testAppendText() {
        Document doc = Parser.parse("<div id=1><p>Hello</p></div>", "");
        Element div = doc.getElementById("1");
        div.appendText(" there & now >");
        assertEquals("Hello there & now >", div.text());
        assertEquals("<p>Hello</p>there &amp; now &gt;", TextUtil.stripNewlines(div.html()));
    }

    @Test
    public void testPrependText() {
        Document doc = Parser.parse("<div id=1><p>Hello</p></div>", "");
        Element div = doc.getElementById("1");
        div.prependText(" there & now >");
        assertEquals("there & now > Hello", div.text());
        assertEquals("there &amp; now &gt;\n<p>Hello</p>", div.html());
    }

    @Test
    public void testThrowsByAppendNull() {
        Document doc = Parser.parse("<div id=1><p>Hello</p></div>", "");
        Element div = doc.getElementById("1");
        assertThrows(IllegalArgumentException.class, () -> div.appendText(null));
    }

    @Test
    public void testSetHtml() {
        Document doc = Parser.parse("<div id=1><p>Hello</p></div>", "");
        Element div = doc.getElementById("1");
        div.html("<p>there</p><p>now</p>");
        assertEquals("<p>there</p><p>now</p>", TextUtil.stripNewlines(div.html()));
    }
}
