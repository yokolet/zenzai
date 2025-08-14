package zenzai.nodes;

import java.util.List;
import java.util.LinkedList;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.*;

import zenzai.helper.Validate;

public abstract class HtmlNode implements Node, Cloneable {
    @Nullable HtmlElement parentNode; // Nodes don't always have parents

    public abstract String getNodeName();
    public abstract String getNodeValue();
    public abstract void setNodeValue(String nodeValue) throws DOMException;
    public abstract short getNodeType();
    public abstract Node getParentNode();
    public abstract NodeList getChildNodes();
    public abstract Node getFirstChild();
    public abstract Node getLastChild();
    public abstract Node getPreviousSibling();
    public abstract Node getNextSibling();
    public abstract NamedNodeMap getAttributes();
    public abstract Document getOwnerDocument();
    public abstract Node insertBefore(Node newChild, Node refChild) throws DOMException;
    public abstract Node replaceChild(Node newChild, Node oldChild) throws DOMException;
    public abstract Node removeChild(Node oldChild) throws DOMException;
    public abstract Node appendChild(Node newChild) throws DOMException;
    public abstract boolean hasChildNodes();
    public abstract Node cloneNode(boolean deep);
    public abstract void normalize();
    public abstract boolean isSupported(String feature, String version);
    public abstract String getNamespaceURI();
    public abstract String getPrefix();
    public abstract void setPrefix(String prefix) throws DOMException;
    public abstract String getLocalName();
    public abstract boolean hasAttributes();
    public abstract String getBaseURI();
    public abstract short compareDocumentPosition(Node other) throws DOMException;
    public abstract String getTextContent() throws DOMException;
    public abstract void setTextContent(String textContent) throws DOMException;
    public abstract boolean isSameNode(Node other);
    public abstract String lookupPrefix(String namespaceURI);
    public abstract boolean isDefaultNamespace(String namespaceURI);
    public abstract String lookupNamespaceURI(String prefix);
    public abstract boolean isEqualNode(Node arg);
    public abstract Object getFeature(String feature, String version);
    public abstract Object setUserData(String key, Object data, UserDataHandler handler);
    public abstract Object getUserData(String key);
    

    @Override
    public HtmlNode clone() {
        HtmlNode thisClone = doClone(null); // splits for orphan

        // Queue up nodes that need their children cloned (BFS).
        final LinkedList<HtmlNode> nodesToProcess = new LinkedList<>();
        nodesToProcess.add(thisClone);

        while (!nodesToProcess.isEmpty()) {
            HtmlNode currParent = nodesToProcess.remove();

            final int size = currParent.childNodeSize();
            for (int i = 0; i < size; i++) {
                final List<HtmlNode> childNodes = currParent.ensureChildNodes();
                HtmlNode childClone = childNodes.get(i).doClone(currParent);
                childNodes.set(i, childClone);
                nodesToProcess.add(childClone);
            }
        }
        return thisClone;
    }

    protected HtmlNode doClone(@Nullable HtmlNode parent) {
        assert parent == null || parent instanceof Element;
        HtmlNode clone;

        try {
            clone = (HtmlNode) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        clone.parentNode = (Element) parent; // can be null, to create an orphan split
        clone.siblingIndex = parent == null ? 0 : siblingIndex();
        // if not keeping the parent, shallowClone the ownerDocument to preserve its settings
        if (parent == null && !(this instanceof Document)) {
            Document doc = ownerDocument();
            if (doc != null) {
                Document docClone = doc.shallowClone();
                clone.parentNode = docClone;
                docClone.ensureChildNodes().add(clone);
            }
        }
        return clone;
    }

    /**
     * Get the number of child nodes that this node holds.
     * @return the number of child nodes that this node holds.
     */
    public abstract int childNodeSize();

    /**
     * Set the baseUri for just this node (not its descendants), if this Node tracks base URIs.
     * @param baseUri new URI
     */
    protected abstract void doSetBaseUri(String baseUri);

    /**
     Update the base URI of this node and all of its descendants.
     @param baseUri base URI to set
     */
    public void setBaseUri(final String baseUri) {
        Validate.notNull(baseUri);
        doSetBaseUri(baseUri);
    }

    /**
     Get the node name of this node. Use for debugging purposes and not logic switching (for that, use instanceof).
     @return node name
     */
    public abstract String nodeName();

    /**
     Get the normalized name of this node. For node types other than Element, this is the same as {@link #nodeName()}.
     For an Element, will be the lower-cased tag name.
     @return normalized node name
     @since 1.15.4.
     */
    public String normalName() {
        return nodeName();
    }

    /**
     * Gets the Document associated with this Node.
     * @return the Document associated with this Node, or null if there is no such Document.
     */
    public @Nullable Document ownerDocument() {
        HtmlNode node = this;
        while (node != null) {
            if (node instanceof Document) return (Document) node;
            node = node.parentNode;
        }
        return null;
    }

    /**
     Gets this node's parent node. Not overridable by extending classes, so useful if you really just need the Node type.
     @return parent node; or null if no parent.
     */
    public @Nullable final HtmlNode parentNode() {
        return parentNode;
    }
}
