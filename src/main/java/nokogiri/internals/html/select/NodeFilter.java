package nokogiri.internals.html.select;

import nokogiri.internals.html.nodes.Node;

public interface NodeFilter {
    /**
     Traversal action.
     */
    enum FilterResult {
        /** Continue processing the tree */
        CONTINUE,
        /** Skip the child nodes, but do call {@link NodeFilter#tail(Node, int)} next. */
        SKIP_CHILDREN,
        /** Skip the subtree, and do not call {@link NodeFilter#tail(Node, int)}. */
        SKIP_ENTIRELY,
        /** Remove the node and its children */
        REMOVE,
        /** Stop processing */
        STOP
    }

    /**
     * Callback for when a node is first visited.
     * @param node the node being visited.
     * @param depth the depth of the node, relative to the root node. E.g., the root node has depth 0, and a child node of that will have depth 1.
     * @return Traversal action
     */
    FilterResult head(Node node, int depth);

    /**
     * Callback for when a node is last visited, after all of its descendants have been visited.
     * <p>This method has a default implementation to return {@link FilterResult#CONTINUE}.</p>
     * @param node the node being visited.
     * @param depth the depth of the node, relative to the root node. E.g., the root node has depth 0, and a child node of that will have depth 1.
     * @return Traversal action
     */
    default FilterResult tail(Node node, int depth) {
        return FilterResult.CONTINUE;
    }

    /**
     Run a depth-first controlled traverse of the root and all of its descendants.
     @param root the initial node point to traverse.
     @since 1.21.1
     */
    default void traverse(Node root) {
        NodeTraversor.filter(this, root);
    }
}
