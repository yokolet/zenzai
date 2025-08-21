package zenzai.select;

import zenzai.nodes.HtmlElement;
import zenzai.nodes.HtmlNode;

@FunctionalInterface
public interface NodeVisitor {
    /**
     Callback for when a node is first visited.
     <p>The node may be modified (e.g. {@link HtmlNode#attr(String)}, replaced {@link HtmlNode#replaceWith(HtmlNode)}) or removed
     {@link HtmlNode#remove()}. If it's {@code instanceOf Element}, you may cast it to an {@link HtmlElement} and access those
     methods.</p>

     @param node the node being visited.
     @param depth the depth of the node, relative to the root node. E.g., the root node has depth 0, and a child node
     of that will have depth 1.
     */
    void head(HtmlNode node, int depth);

    /**
     Callback for when a node is last visited, after all of its descendants have been visited.
     <p>This method has a default no-op implementation.</p>
     <p>Note that neither replacement with {@link HtmlNode#replaceWith(Node)} nor removal with {@link HtmlNode#remove()} is
     supported during {@code tail()}.

     @param node the node being visited.
     @param depth the depth of the node, relative to the root node. E.g., the root node has depth 0, and a child node
     of that will have depth 1.
     */
    default void tail(HtmlNode node, int depth) {
        // no-op by default, to allow just specifying the head() method
    }

    /**
     Run a depth-first traverse of the root and all of its descendants.
     @param root the initial node point to traverse.
     @since 1.21.1
     */
    default void traverse(HtmlNode root) {
        NodeTraversor.traverse(this, root);
    }
}
