package zenzai.nodes;

import zenzai.parser.Parser;
import zenzai.parser.HtmlTreeBuilder;

final class NodeUtils {

    /**
     * Get the output setting for this node,  or if this node has no document (or parent), retrieve the default output
     * settings
     */
    static Document.OutputSettings outputSettings(Node node) {
        Document owner = node.ownerDocument();
        return owner != null ? owner.outputSettings() : (new Document("")).outputSettings();
    }

    /**
     * Get the parser that was used to make this node, or the default HTML parser if it has no parent.
     */
    static Parser parser(zenzai.nodes.Node node) {
        Document doc = node.ownerDocument();
        return doc != null ? doc.parser() : new Parser(new HtmlTreeBuilder());
    }
}
