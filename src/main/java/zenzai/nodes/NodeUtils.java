package zenzai.nodes;

import zenzai.parser.HtmlParser;
import zenzai.parser.HtmlTreeBuilder;

final class NodeUtils {
    /**
     * Get the parser that was used to make this node, or the default HTML parser if it has no parent.
     */
    static HtmlParser parser(HtmlNode node) {
        HtmlDocument doc = node.ownerDocument();
        return doc != null ? doc.parser() : new HtmlParser(new HtmlTreeBuilder());
    }
}
