package zenzai.nodes;

import org.junit.jupiter.api.Test;
import zenzai.TextUtil;
import zenzai.parser.Parser;
import zenzai.select.Elements;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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

    @Test
    public void testDataset() {
        String html = "<div id=1 data-name=jsoup class=new data-package=jar>Hello</div><p id=2>Hello</p>";
        Document doc = Parser.parse(html, "");
        Element div = doc.getElementById("1");
        Map<String, String> dataset = div.dataset();
        Attributes attributes = div.attributes();

        assertEquals(2, dataset.size());
        assertEquals(4, attributes.size());
        assertEquals("jsoup", dataset.get("name"));
        assertEquals("jar", dataset.get("package"));

        dataset.put("name", "jsoup updated");
        dataset.put("language", "java");
        dataset.remove("package");

        assertEquals(2, dataset.size());
        assertEquals(4, attributes.size());
        assertEquals("jsoup updated", attributes.get("data-name"));
        assertEquals("jsoup updated", dataset.get("name"));
        assertEquals("java", attributes.get("data-language"));
        assertEquals("java", dataset.get("language"));

        Element el = doc.getElementById("2");
        assertEquals(0, el.dataset().size());
    }

    @Test
    public void testClone() {
        Document doc = Parser.parse("<div><p>One<p><span>Two</div>", "");
        Element p = doc.getElementsByTag("p").get(1);
        Element clone = p.clone();

        assertNotNull(clone.parentNode);
        assertEquals(1, clone.parentNode.childNodeSize());
        assertSame(clone.ownerDocument(), clone.parentNode);

        assertEquals(0, clone.siblingIndex);
        assertEquals(1, p.siblingIndex);
        assertNotNull(p.parent());

        clone.append("<span>Three");
        assertEquals("<p><span>Two</span><span>Three</span></p>", TextUtil.stripNewlines(clone.outerHtml()));
        assertEquals("<div><p>One</p><p><span>Two</span></p></div>", TextUtil.stripNewlines(doc.body().html())); // not modified
    }

    @Test
    public void testShallowClone() {
        String baseUri = "http://example.com/";
        Document doc = Parser.parse("<div id=1 class=one><p id=2 class=two>One", baseUri);
        Element div = doc.getElementById("1");
        Element p = doc.getElementById("2");
        TextNode text = p.textNodes().get(0);

        Element div2 = div.shallowClone();
        Element p2 = p.shallowClone();
        TextNode text2 = (TextNode) text.shallowClone();

        assertEquals(1, div.childNodeSize());
        assertEquals(0, div2.childNodeSize());

        assertEquals(1, p.childNodeSize());
        assertEquals(0, p2.childNodeSize());

        assertEquals("", p2.text());
        assertEquals("One", text2.text());

        assertEquals(baseUri, div2.baseUri());
    }

    @Test
    public void testMoveByAppend() {
        Document doc = Parser.parse("<div id=1>Text <p>One</p> Text <p>Two</p></div><div id=2></div>", "");
        Element div1 = doc.getElementById("1");
        Element div2 = doc.getElementById("2");

        assertEquals(4, div1.childNodeSize());
        assertEquals(0, div2.childNodeSize());

        List<Node> children = div1.childNodes();
        assertEquals(4, children.size());

        div2.insertChildren(0, children);

        assertEquals(4, children.size());
        assertEquals(0, div1.childNodeSize());
        assertEquals(4, div2.childNodeSize());
        String expected = "<div id=\"1\"></div>\n<div id=\"2\">\n Text\n <p>One</p>\n Text\n <p>Two</p>\n</div>";
        assertEquals(expected, doc.body().html());
    }

    @Test
    public void testInsertChildrenArgument() {
        Document doc = Parser.parse("<div id=1>Text <p>One</p> Text <p>Two</p></div><div id=2></div>", "");
        Element div1 = doc.getElementById("1");
        Element div2 = doc.getElementById("2");
        List<Node> children = div1.childNodes();

        assertThrows(IllegalArgumentException.class, () -> div2.insertChildren(6, children));
        assertThrows(IllegalArgumentException.class, () -> div2.insertChildren(-5, children));
        assertThrows(IllegalArgumentException.class, () -> div2.insertChildren(0, (Collection<? extends Node>) null));
    }

    @Test
    public void testInsertCopiedChildren() {
        Document doc = Parser.parse("<div id=1>Text <p>One</p> Text <p>Two</p></div><div id=2></div>", "");
        Element div1 = doc.getElementById("1");
        Element div2 = doc.getElementById("2");
        Elements children = doc.getElementsByTag("p").clone();
        children.first().text("One cloned");
        div2.insertChildren(-1, children);

        assertEquals(4, div1.childNodeSize());
        assertEquals(2, div2.childNodeSize());
        String expected = "<div id=\"1\">\n Text\n <p>One</p>\n Text\n <p>Two</p>\n</div>\n<div id=\"2\">\n <p>One cloned</p>\n <p>Two</p>\n</div>";
        assertEquals(expected, doc.body().html());
    }

    @Test
    public void testRemoveAttributes() {
        String html = "<a one two three four>Text</a><p foo>Two</p>";
        Document doc = Parser.parse(html, "");
        for (Element el : doc.getAllElements()) {
            el.clearAttributes();
        }

        assertEquals("<a>Text</a>\n<p>Two</p>", doc.body().html());
    }

    @Test
    public void testRemoveAttr() {
        Element el = new Element("a")
                .attr("href", "http://example.com")
                .attr("id", "1")
                .text("Hello");
        assertEquals("<a href=\"http://example.com\" id=\"1\">Hello</a>", el.outerHtml());
        Element el2 = el.removeAttr("href");
        assertSame(el, el2);
        assertEquals("<a id=\"1\">Hello</a>", el2.outerHtml());
    }

    @Test
    public void testRoot() {
        Element el = new Element("a");
        el.append("<span>Hello</span>");
        assertEquals("<a><span>Hello</span></a>", el.outerHtml());

        Element span = el.getElementsByTag("span").get(0);
        assertNotNull(span);
        assertSame(el, span.root());

        Document doc = Parser.parse("<div><p>One<p>Two<p>Three", "");
        Element div = doc.getElementsByTag("div").get(0);
        assertSame(doc, div.root());
        assertSame(doc, div.ownerDocument());
    }

    @Test
    public void testChildIndex() {
        Document doc = Parser.parse("<div>One <p>Two</p> Three <p>Four</p> Five <p>Six</p>", "");
        Element div = doc.getElementsByTag("div").get(0);
        Element child0 = div.child(0);
        Element child1 = div.child(1);
        Element child2 = div.child(2);
        assertNull(div.cachedChildren());

        assertEquals("Two", child0.text());
        assertEquals("Four", child1.text());
        assertEquals("Six", child2.text());

        Elements children = div.children();
        assertNotNull(div.cachedChildren());;
    }
}
