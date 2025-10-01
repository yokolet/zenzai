package zenzai.nodes;

import org.junit.jupiter.api.Test;
import zenzai.TextUtil;
import zenzai.parser.Parser;

import static org.junit.jupiter.api.Assertions.*;

public class TextNodeTest {
    @Test
    public void testBlank() {
        TextNode one = new TextNode("");
        TextNode two = new TextNode("     ");
        TextNode three = new TextNode("  \n\n   ");
        TextNode four = new TextNode("Hello");
        TextNode five = new TextNode("  \nHello ");

        assertTrue(one.isBlank());
        assertTrue(two.isBlank());
        assertTrue(three.isBlank());
        assertFalse(four.isBlank());
        assertFalse(five.isBlank());
    }

    @Test
    public void testTextValues() {
        Document doc = Parser.parse("<p>One <span>two &amp;</span> three &amp;</p>", "");
        Element p = doc.getElementsByTag("p").first();
        Element span = doc.getElementsByTag("span").first();

        assertEquals("two &", span.text());
        TextNode text = (TextNode) span.childNode(0);
        assertEquals("two &", text.text());

        TextNode text2 = (TextNode) p.childNode(2);
        assertEquals(" three &", text2.text());

        text2.text(" POW!");
        assertEquals("One <span>two &amp;</span> POW!", TextUtil.stripNewlines(p.html()));

        text2.attr(text2.nodeName(), "kablam &");
        assertEquals("kablam &", text2.text());
        assertEquals("One <span>two &amp;</span>kablam &amp;", TextUtil.stripNewlines(p.html()));
    }

    @Test
    public void testSplitText() {
        Document doc = Parser.parse("<div>Hello there</div>", "");
        Element div = doc.getElementsByTag("div").first();
        TextNode text = (TextNode) div.childNode(0);
        TextNode tail = (TextNode) text.splitText(6);
        assertEquals("Hello ", text.getWholeText());
        assertEquals("there", tail.getWholeText());
        tail.text("there!");
        assertEquals("Hello there!", div.text());
        assertSame(text.parent(), tail.parent());
        assertThrows(org.w3c.dom.DOMException.class, () -> text.splitText(-5));
        assertThrows(org.w3c.dom.DOMException.class, () -> text.splitText(100));
    }

    @Test
    public void testWrap() {
        Document doc = Parser.parse("<div>Hello there</div>", "");
        Element div = doc.getElementsByTag("div").first();
        TextNode text = (TextNode) div.childNode(0);
        TextNode tail = (TextNode) text.splitText(6);
        tail.wrap("<b></b>");

        assertEquals("Hello <b>there</b>", div.html());
    }

    @Test
    public void testClone() {
        TextNode x = new TextNode("xxx");
        TextNode y = x.clone();

        assertNotSame(x, y);
        assertEquals(x.outerHtml(), y.outerHtml());

        y.text("yyy");
        assertNotEquals(x.outerHtml(), y.outerHtml());
        assertEquals("xxx", x.text());
        assertEquals("yyy", y.text());
    }
}
