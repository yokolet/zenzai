package nokogiri.internals.html.nodes;

import org.junit.jupiter.api.Test;
import nokogiri.internals.html.parser.Parser;
import nokogiri.internals.html.select.NodeFilter;

import static org.junit.jupiter.api.Assertions.*;

public class LeafNodeTest {
    @Test
    public void testLeafNodes() {
        String html = "<p>One <!-- Two --> Three<![CDATA[Four]]></p>";
        Document doc = Parser.parse(html, "https://example.com");
        Element p = (Element) doc.getElementsByTag("p").first();
        assertEquals(4, p.childNodeSize());

        Node n0 = p.childNode(0);
        assertTrue(n0 instanceof TextNode);
        Node n1 = p.childNode(1);
        assertTrue(n1 instanceof Comment);
        Node n2 = p.childNode(2);
        assertTrue(n2 instanceof TextNode);
        Node n3 = p.childNode(3);
        assertTrue(n3 instanceof CDataNode);
    }

    @Test
    public void testAttributes() {
        String html = "<p>One <!-- Two --> Three<![CDATA[Four]]></p>";
        Document doc = Parser.parse(html, "https://example.com");
        assertTrue(hasAnyAttributes(doc));  // Base Uri is an attribute

        Element p = doc.getElementsByTag("p").first();
        assertFalse(hasAnyAttributes(p));

        p.attr("class", "foo");
        assertTrue(hasAnyAttributes(p));
    }

    private boolean hasAnyAttributes(Node node) {
        final boolean[] found = new boolean[1];
        node.filter(new NodeFilter() {
            @Override
            public FilterResult head(Node node, int depth) {
                if (node.hasAttributes()) {
                    found[0] = true;
                    return FilterResult.STOP;
                } else {
                    return FilterResult.CONTINUE;
                }
            }

            @Override
            public FilterResult tail(Node node, int depth) {
                return FilterResult.CONTINUE;
            }
        });
        return found[0];
    }
}
