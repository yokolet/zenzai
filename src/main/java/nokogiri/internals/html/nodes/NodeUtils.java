package nokogiri.internals.html.nodes;

import nokogiri.internals.html.parser.Parser;
import nokogiri.internals.html.parser.HtmlTreeBuilder;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
    static Parser parser(nokogiri.internals.html.nodes.Node node) {
        Document doc = node.ownerDocument();
        return doc != null ? doc.parser() : new Parser(new HtmlTreeBuilder());
    }

    /** Creates a Stream, starting with the supplied node. */
    static <T extends Node> Stream<T> stream(Node start, Class<T> type) {
        NodeIterator<T> iterator = new NodeIterator<>(start, type);
        Spliterator<T> spliterator = spliterator(iterator);

        return StreamSupport.stream(spliterator, false);
    }

    static <T extends Node> Spliterator<T> spliterator(Iterator<T> iterator) {
        return Spliterators.spliteratorUnknownSize(
                iterator,
                Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.ORDERED);
    }
}
