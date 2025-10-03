package nokogiri.internals.html.nodes;

import org.junit.jupiter.api.Test;
import nokogiri.internals.html.TextUtil;
import nokogiri.internals.html.parser.Parser;
import nokogiri.internals.html.parser.Tag;
import nokogiri.internals.html.select.Elements;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NodeTest {
    @Test
    public void testBaseUri() {
        Tag tag = Tag.valueOf("a");
        Attributes attributes = new Attributes();
        attributes.put("relHref", "/foo");
        attributes.put("absHref", "http://bar/qux");

        Element el = new Element(tag, "", attributes);
        assertEquals("", el.absUrl("relHref"));
        assertEquals("http://bar/qux", el.absUrl("absHref"));

        el = new Element(tag, "http://foo/", attributes);
        assertEquals("http://foo/foo", el.absUrl("relHref"));
        assertEquals("http://bar/qux", el.absUrl("absHref"));
        assertEquals("http://foo/", el.baseUri());
    }

    @Test
    public void setBaseUri() {
        Document doc = Parser.parse("<div><p></p></div>", "");
        String baseUri = "https://jsoup.org";
        doc.setBaseUri(baseUri);

        assertEquals(baseUri, doc.baseUri());
        assertEquals(baseUri, doc.getElementsByTag("div").first().baseUri());
        assertEquals(baseUri, doc.getElementsByTag("p").first().baseUri());
    }

    @Test
    public void testRemoveNode() {
        Document doc = Parser.parse("<p>One <span>two</span> three</p>", "");
        Element p = doc.getElementsByTag("p").first();
        p.childNode(0).remove();

        assertEquals("two three", p.text());
        assertEquals("<span>two</span> three", TextUtil.stripNewlines(p.html()));
    }

    @Test
    public void testReplaceNode() {
        Document doc = Parser.parse("<p>One <span>two</span> three</p>", "");
        Element p = doc.getElementsByTag("p").first();
        Element newNode = doc.createElement("em").text("foo");
        p.childNode(1).replaceWith(newNode);

        assertEquals("One <em>foo</em> three", p.html());
    }

    @Test
    public void testReplaceNodeValue() {
        Document doc = Parser.parse("<p><span>Child One</span><span>Child Two</span><span>Child Three</span><span>Child Four</span></p>", "");
        Elements children = doc.getElementsByTag("p").first().children();
        assertEquals("Child OneChild TwoChild ThreeChild Four", TextUtil.stripNewlines(children.html()));

        children.set(0, children.set(1, children.get(0)));
        assertEquals("Child TwoChild OneChild ThreeChild Four", TextUtil.stripNewlines(children.html()));

        children.set(2, children.set(1, children.get(2)));
        assertEquals("Child TwoChild ThreeChild OneChild Four", TextUtil.stripNewlines(children.html()));
    }

    @Test
    public void testOwnerDocument() {
        Document doc = Parser.parse("<p>Hello", "");
        Element p = doc.getElementsByTag("p").first();
        assertSame(p.ownerDocument(), doc);
        assertSame(doc.ownerDocument(), doc);
        assertNull(doc.parent());
    }

    @Test
    public void testRoot() {
        Document doc = Parser.parse("<p>Hello", "");
        Element p = doc.getElementsByTag("p").first();
        Node root = p.root();
        assertSame(doc, root);
        assertNull(root.parent());
        assertSame(doc.root(), doc.ownerDocument());

        Element el = new Element(Tag.valueOf("p"), "");
        assertNull(el.parent());
        assertNull(el.ownerDocument());
    }

    @Test
    public void testBefore() {
        Document doc = Parser.parse("<p>One <b>two</b> three</p>", "");
        Element newNode = new Element(Tag.valueOf("em"), "");
        newNode.appendText("four");

        doc.getElementsByTag("b").first().before(newNode);
        assertEquals("<p>One <em>four</em><b>two</b> three</p>", doc.body().html());

        doc.getElementsByTag("b").first().before("<i>five</i>");
        assertEquals("<p>One <em>four</em><i>five</i><b>two</b> three</p>", doc.body().html());
    }

    @Test
    public void testAfter() {
        Document doc = Parser.parse("<p>One <b>two</b> three</p>", "");
        Element newNode = new Element(Tag.valueOf("em"), "");
        newNode.appendText("four");

        doc.getElementsByTag("b").first().after(newNode);
        assertEquals("<p>One <b>two</b><em>four</em> three</p>", doc.body().html());

        doc.getElementsByTag("b").first().after("<i>five</i>");
        assertEquals("<p>One <b>two</b><i>five</i><em>four</em> three</p>", doc.body().html());
    }

    @Test
    public void testUnwrap() {
        Document doc = Parser.parse("<div>One <span>Two <b>Three</b></span> Four</div>", "");
        Element span = doc.getElementsByTag("span").first();
        Node node = span.unwrap();

        assertEquals("<div>One Two <b>Three</b> Four</div>", TextUtil.stripNewlines(doc.body().html()));
        assertTrue(node instanceof TextNode);
        assertEquals("Two ", ((TextNode)node).text());
        assertEquals(node.parent(), doc.getElementsByTag("div").first());
    }

    @Test
    public void testOrphanNode() {
        Node node = new Element(Tag.valueOf("p"), "");
        assertEquals(0, node.siblingIndex());
        assertEquals(0, node.siblingNodes().size());
        assertNull(node.previousSibling());
        assertNull(node.nextSibling());
    }

    @Test
    public void testCopyChildren() {
        String html = "<div id=1>Text 1 <p>One</p> Text 2 <p>Two<p>Three</div><div id=2>";
        Document doc = Parser.parse(html, "");
        Element div1 = doc.getElementById("1");
        Element div2 = doc.getElementById("2");
        List<Node> copied = div1.childNodesCopy();
        assertEquals(5, copied.size());

        TextNode text1 = (TextNode) div1.childNode(0);
        TextNode text2 = (TextNode) copied.get(0);
        text2.text("Text 1 updated");
        assertEquals("Text 1 ", text1.text());
        assertEquals("Text 1 updated", text2.text());

        div2.insertChildren(-1, copied);
        String expected = "<div id=\"1\">Text 1<p>One</p>Text 2<p>Two</p><p>Three</p></div><div id=\"2\">Text 1 updated<p>One</p>Text 2<p>Two</p><p>Three</p></div>";
        assertEquals(expected, TextUtil.stripNewlines(doc.body().html()));
    }

    @Test
    public void testClone() {
        Document doc = Parser.parse("<div class=foo>Text</div>", "");
        Element div = doc.getElementsByTag("div").first();
        assertTrue(div.hasClass("foo"));

        Element cloned = doc.clone().getElementsByTag("div").first();
        assertTrue(cloned.hasClass("foo"));

        div.text("None");
        assertEquals("None", div.text());
        assertEquals("Text", cloned.text());
    }
}
