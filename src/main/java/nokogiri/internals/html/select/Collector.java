package nokogiri.internals.html.select;

import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import nokogiri.internals.html.nodes.Element;
import nokogiri.internals.html.nodes.LeafNode;
import nokogiri.internals.html.nodes.Node;
import nokogiri.internals.html.nodes.TextNode;
import nokogiri.internals.html.select.Elements;
import nokogiri.internals.html.select.Evaluator;

import static java.util.stream.Collectors.toCollection;

/**
 * Collects a list of elements that match the supplied criteria.
 *
 * @author Jonathan Hedley
 */
public class Collector {
    private Collector() {}

    /**
     Build a list of elements, by visiting the root and every descendant of root, and testing it against the Evaluator.
     @param eval Evaluator to test elements against
     @param root root of tree to descend
     @return list of matches; empty if none
     */
    public static Elements collect(Evaluator eval, Element root) {
        Stream<Element> stream = eval.wantsNodes() ?
                streamNodes(eval, root, Element.class) :
                stream(eval, root);

        return stream.collect(toCollection(Elements::new));
    }

    /**
     Obtain a Stream of elements by visiting the root and every descendant of root and testing it against the evaluator.

     @param evaluator Evaluator to test elements against
     @param root root of tree to descend
     @return A {@link Stream} of matches
     @since 1.19.1
     */
    public static Stream<Element> stream(Evaluator evaluator, Element root) {
        evaluator.reset();
        return root.stream().filter(evaluator.asPredicate(root));
    }

    /**
     Obtain a Stream of nodes, of the specified type, by visiting the root and every descendant of root and testing it
     against the evaluator.

     @param evaluator Evaluator to test elements against
     @param root root of tree to descend
     @param type the type of node to collect (e.g. {@link Element}, {@link LeafNode}, {@link TextNode} etc)
     @param <T> the type of node to collect
     @return A {@link Stream} of matches
     @since 1.21.1
     */
    public static <T extends Node> Stream<T> streamNodes(Evaluator evaluator, Element root, Class<T> type) {
        evaluator.reset();
        return root.nodeStream(type).filter(evaluator.asNodePredicate(root));
    }

    /**
     Finds the first Element that matches the Evaluator that descends from the root, and stops the query once that first
     match is found.
     @param eval Evaluator to test elements against
     @param root root of tree to descend
     @return the first match; {@code null} if none
     */
    public static @Nullable Element findFirst(nokogiri.internals.html.select.Evaluator eval, Element root) {
        return stream(eval, root).findFirst().orElse(null);
    }
}