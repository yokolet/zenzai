package zenzai.nodes;

import zenzai.parser.Parser;
import zenzai.parser.HtmlTreeBuilder;

final class NodeUtils {
    /**
     * Get the parser that was used to make this node, or the default HTML parser if it has no parent.
     */
    static Parser parser(zenzai.nodes.Node node) {
        HtmlDocument doc = node.ownerDocument();
        return doc != null ? doc.parser() : new Parser(new HtmlTreeBuilder());
    }
}
