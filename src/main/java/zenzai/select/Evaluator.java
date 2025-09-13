package zenzai.select;

import zenzai.nodes.LeafNode;
import zenzai.nodes.Node;
import zenzai.nodes.Element;

import java.util.function.Predicate;

/**
 An Evaluator tests if an element (or a node) meets the selector's requirements. Obtain an evaluator for a given CSS selector
 with {@link Selector#evaluatorOf(String css)}. If you are executing the same selector on many elements (or documents), it
 can be more efficient to compile and reuse an Evaluator than to reparse the selector on each invocation of select().
 <p>Evaluators are thread-safe and may be used concurrently across multiple documents.</p>
 */
public abstract class Evaluator {
    protected Evaluator() {}

    /**
     Provides a Predicate for this Evaluator, matching the test Element.
     * @param root the root Element, for match evaluation
     * @return a predicate that accepts an Element to test for matches with this Evaluator
     * @since 1.17.1
     */
    public Predicate<zenzai.nodes.Element> asPredicate(zenzai.nodes.Element root) {
        return element -> matches(root, element);
    }

    Predicate<zenzai.nodes.Node> asNodePredicate(zenzai.nodes.Element root) {
        return node -> matches(root, node);
    }

    /**
     * Test if the element meets the evaluator's requirements.
     *
     * @param root    Root of the matching subtree
     * @param element tested element
     * @return Returns <tt>true</tt> if the requirements are met or
     * <tt>false</tt> otherwise
     */
    public abstract boolean matches(zenzai.nodes.Element root, zenzai.nodes.Element element);

    /**
     A relative evaluator cost function. During evaluation, Evaluators are sorted by ascending cost as an optimization.
     * @return the relative cost of this Evaluator
     */
    protected int cost() {
        return 5; // a nominal default cost
    }

    /**
     Reset any internal state in this Evaluator before executing a new Collector evaluation.
     */
    protected void reset() {
    }

    final boolean matches(zenzai.nodes.Element root, Node node) {
        if (node instanceof zenzai.nodes.Element) {
            return matches(root, (zenzai.nodes.Element) node);
        } else if (node instanceof LeafNode && wantsNodes()) {
            return matches(root, (LeafNode) node);
        }
        return false;
    }

    boolean matches(zenzai.nodes.Element root, LeafNode leafNode) {
        return false;
    }

    boolean wantsNodes() {
        return false;
    }

    /**
     * Evaluator for tag name
     */
    public static final class Tag extends Evaluator {
        private final String tagName;

        public Tag(String tagName) {
            this.tagName = tagName;
        }

        @Override
        public boolean matches(Element root, Element element) {
            return (element.nameIs(tagName));
        }

        @Override protected int cost() {
            return 1;
        }

        @Override
        public String toString() {
            return String.format("%s", tagName);
        }
    }
}