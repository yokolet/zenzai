package zenzai.select;

import zenzai.nodes.Element;
import zenzai.nodes.Node;

@FunctionalInterface
public interface NodeVisitor {
    /**
     Callback for when a node is first visited.
     <p>The node may be modified (e.g. {@link Node#attr(String)}, replaced {@link Node#replaceWith(Node)}) or removed
     {@link Node#remove()}. If it's {@code instanceOf Element}, you may cast it to an {@link Element} and access those
     methods.</p>

     @param node the node being visited.
     @param depth the depth of the node, relative to the root node. E.g., the root node has depth 0, and a child node
     of that will have depth 1.
     */
    void head(Node node, int depth);

    /**
     Callback for when a node is last visited, after all of its descendants have been visited.
     <p>This method has a default no-op implementation.</p>
     <p>Note that neither replacement with {@link Node#replaceWith(Node)} nor removal with {@link Node#remove()} is
     supported during {@code tail()}.

     @param node the node being visited.
     @param depth the depth of the node, relative to the root node. E.g., the root node has depth 0, and a child node
     of that will have depth 1.
     */
    default void tail(Node node, int depth) {
        // no-op by default, to allow just specifying the head() method
    }

    /**
     Run a depth-first traverse of the root and all of its descendants.
     @param root the initial node point to traverse.
     @since 1.21.1
     */
    default void traverse(Node root) {
        NodeTraversor.traverse(this, root);
    }
}
