package nokogiri.internals.html.select;

import nokogiri.internals.html.helper.Validate;
import nokogiri.internals.html.nodes.Element;
import nokogiri.internals.html.nodes.Node;
import nokogiri.internals.html.select.NodeFilter.FilterResult;

public class NodeTraversor {
    /**
     Run a depth-first traverse of the root and all of its descendants.
     @param visitor Node visitor.
     @param root the initial node point to traverse.
     @see NodeVisitor#traverse(Node root)
     */
    public static void traverse(NodeVisitor visitor, Node root) {
        Validate.notNull(visitor);
        Validate.notNull(root);
        Node node = root;
        int depth = 0;

        while (node != null) {
            Node parent = node.parentNode(); // remember parent to find nodes that get replaced in .head
            int origSize = parent != null ? parent.childNodeSize() : 0;
            Node next = node.nextSibling();

            visitor.head(node, depth); // visit current node

            // check for modifications to the tree
            if (parent != null && !node.hasParent()) { // removed or replaced
                if (origSize == parent.childNodeSize()) { // replaced
                    node = parent.childNode(node.siblingIndex()); // replace ditches parent but keeps sibling index
                    continue;
                }
                // else, removed
                node = next;
                if (node == null) {
                    // was last in parent. need to walk up the tree, tail()ing on the way, until we find a suitable next. Otherwise, would revisit ancestor nodes.
                    node = parent;
                    while (true) {
                        depth--;
                        visitor.tail(node, depth);
                        if (node == root) break;
                        if (node.nextSibling() != null) {
                            node = node.nextSibling();
                            break;
                        }
                        node = node.parentNode();
                        if (node == null) break;
                    }
                    if (node == root || node == null) break; // done, break outer
                }
                continue; // don't tail removed
            }

            if (node.childNodeSize() > 0) { // descend
                node = node.childNode(0);
                depth++;
            } else {
                while (true) {
                    assert node != null; // as depth > 0, will have parent
                    if (!(node.nextSibling() == null && depth > 0)) break;
                    visitor.tail(node, depth); // when no more siblings, ascend
                    node = node.parentNode();
                    depth--;
                }
                visitor.tail(node, depth);
                if (node == root)
                    break;
                node = node.nextSibling();
            }
        }
    }

    /**
     Run a depth-first traversal of each Element.
     @param visitor Node visitor.
     @param elements Elements to traverse.
     */
    public static void traverse(NodeVisitor visitor, Elements elements) {
        Validate.notNull(visitor);
        Validate.notNull(elements);
        for (Element el : elements)
            traverse(visitor, el);
    }

    /**
     Run a depth-first filtered traversal of the root and all of its descendants.
     @param filter NodeFilter visitor.
     @param root the root node point to traverse.
     @return The filter result of the root node, or {@link FilterResult#STOP}.

     @see NodeFilter
     */
    public static FilterResult filter(NodeFilter filter, Node root) {
        Node node = root;
        int depth = 0;

        while (node != null) {
            FilterResult result = filter.head(node, depth);
            if (result == FilterResult.STOP)
                return result;
            // Descend into child nodes:
            if (result == FilterResult.CONTINUE && node.childNodeSize() > 0) {
                node = node.childNode(0);
                ++depth;
                continue;
            }
            // No siblings, move upwards:
            while (true) {
                assert node != null; // depth > 0, so has parent
                if (!(node.nextSibling() == null && depth > 0)) break;
                // 'tail' current node:
                if (result == FilterResult.CONTINUE || result == FilterResult.SKIP_CHILDREN) {
                    result = filter.tail(node, depth);
                    if (result == FilterResult.STOP)
                        return result;
                }
                Node prev = node; // In case we need to remove it below.
                node = node.parentNode();
                depth--;
                if (result == FilterResult.REMOVE)
                    prev.remove(); // Remove AFTER finding parent.
                result = FilterResult.CONTINUE; // Parent was not pruned.
            }
            // 'tail' current node, then proceed with siblings:
            if (result == FilterResult.CONTINUE || result == FilterResult.SKIP_CHILDREN) {
                result = filter.tail(node, depth);
                if (result == FilterResult.STOP)
                    return result;
            }
            if (node == root)
                return result;
            Node prev = node; // In case we need to remove it below.
            node = node.nextSibling();
            if (result == FilterResult.REMOVE)
                prev.remove(); // Remove AFTER finding sibling.
        }
        // root == null?
        return FilterResult.CONTINUE;
    }

    /**
     Run a depth-first filtered traversal of each Element.
     @param filter NodeFilter visitor.
     @see NodeFilter
     */
    public static void filter(NodeFilter filter, Elements elements) {
        Validate.notNull(filter);
        Validate.notNull(elements);
        for (Element el : elements)
            if (filter(filter, el) == FilterResult.STOP)
                break;
    }
}
