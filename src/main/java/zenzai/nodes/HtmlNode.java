package zenzai.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.*;

import zenzai.helper.Validate;
import zenzai.internal.StringUtil;

public abstract class HtmlNode implements Node, Cloneable {
    @Nullable HtmlElement parentNode; // Nodes don't always have parents
    static final List<HtmlNode> EmptyNodes = Collections.emptyList();
    static final String EmptyString = "";
    int siblingIndex;

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

    protected abstract List<Node> ensureChildNodes();

    /**
     * Get the number of child nodes that this node holds.
     * @return the number of child nodes that this node holds.
     */
    public abstract int childNodeSize();

    /**
     Get the node name of this node. Use for debugging purposes and not logic switching (for that, use instanceof).
     @return node name
     */
    public abstract String nodeName();

    /**
     * Set the baseUri for just this node (not its descendants), if this Node tracks base URIs.
     * @param baseUri new URI
     */
    protected abstract void doSetBaseUri(String baseUri);

    /**
     Get the base URI that applies to this node. Will return an empty string if not defined. Used to make relative links
     absolute.

     @return base URI
     @see #absUrl
     */
    public abstract String baseUri();

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

        clone.parentNode = (HtmlElement) parent; // can be null, to create an orphan split
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
     * Get the list index of this node in its node sibling list. E.g. if this is the first node
     * sibling, returns 0.
     * @return position in node sibling list
     * @see org.jsoup.nodes.Element#elementSiblingIndex()
     */
    public int siblingIndex() {
        if (parentNode != null && !parentNode.childNodes.validChildren)
            parentNode.reindexChildren();

        return siblingIndex;
    }

    /**
     Update the base URI of this node and all of its descendants.
     @param baseUri base URI to set
     */
    public void setBaseUri(final String baseUri) {
        Validate.notNull(baseUri);
        doSetBaseUri(baseUri);
    }

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

    /**
     Get this node's children. Presented as an unmodifiable list: new children can not be added, but the child nodes
     themselves can be manipulated.
     @return list of children. If no children, returns an empty list.
     */
    public List<HtmlNode> childNodes() {
        if (childNodeSize() == 0)
            return EmptyNodes;

        List<HtmlNode> children = ensureChildNodes();
        List<HtmlNode> rewrap = new ArrayList<>(children.size()); // wrapped so that looping and moving will not throw a CME as the source changes
        rewrap.addAll(children);
        return Collections.unmodifiableList(rewrap);
    }

    /**
     Retrieves this node's sibling nodes. Similar to {@link #childNodes() node.parent.childNodes()}, but does not
     include this node (a node is not a sibling of itself).
     @return node siblings. If the node has no parent, returns an empty list.
     */
    public List<HtmlNode> siblingNodes() {
        if (parentNode == null)
            return Collections.emptyList();

        List<HtmlNode> nodes = parentNode.ensureChildNodes();
        List<HtmlNode> siblings = new ArrayList<>(nodes.size() - 1);
        for (HtmlNode node: nodes)
            if (node != this)
                siblings.add(node);
        return siblings;
    }

    /**
     Test if this node has the specified normalized name, in any namespace.
     * @param normalName a normalized element name (e.g. {@code div}).
     * @return true if the element's normal name matches exactly
     * @since 1.17.2
     */
    public boolean nameIs(String normalName) {
        return normalName().equals(normalName);
    }

    /**
     * Test if this Node has an attribute. <b>Case insensitive</b>.
     * @param attributeKey The attribute key to check.
     * @return true if the attribute exists, false if not.
     */
    public boolean hasAttr(String attributeKey) {
        Validate.notNull(attributeKey);
        if (!hasAttributes())
            return false;

        if (attributeKey.startsWith("abs:")) {
            String key = attributeKey.substring("abs:".length());
            if (attributes().hasKeyIgnoreCase(key) && !absUrl(key).isEmpty())
                return true;
        }
        return attributes().hasKeyIgnoreCase(attributeKey);
    }

    protected void setSiblingIndex(int siblingIndex) {
        this.siblingIndex = siblingIndex;
    }

    /**
     * Get an attribute's value by its key. <b>Case insensitive</b>
     * <p>
     * To get an absolute URL from an attribute that may be a relative URL, prefix the key with <code><b>abs:</b></code>,
     * which is a shortcut to the {@link #absUrl} method.
     * </p>
     * E.g.:
     * <blockquote><code>String url = a.attr("abs:href");</code></blockquote>
     *
     * @param attributeKey The attribute key.
     * @return The attribute, or empty string if not present (to avoid nulls).
     * @see #attributes()
     * @see #hasAttr(String)
     * @see #absUrl(String)
     */
    public String attr(String attributeKey) {
        Validate.notNull(attributeKey);
        if (!hasAttributes())
            return EmptyString;

        String val = attributes().getIgnoreCase(attributeKey);
        if (val.length() > 0)
            return val;
        else if (attributeKey.startsWith("abs:"))
            return absUrl(attributeKey.substring("abs:".length()));
        else return "";
    }

    /**
     * Get an absolute URL from a URL attribute that may be relative (such as an <code>&lt;a href&gt;</code> or
     * <code>&lt;img src&gt;</code>).
     * <p>
     * E.g.: <code>String absUrl = linkEl.absUrl("href");</code>
     * </p>
     * <p>
     * If the attribute value is already absolute (i.e. it starts with a protocol, like
     * <code>http://</code> or <code>https://</code> etc), and it successfully parses as a URL, the attribute is
     * returned directly. Otherwise, it is treated as a URL relative to the element's {@link #baseUri}, and made
     * absolute using that.
     * </p>
     * <p>
     * As an alternate, you can use the {@link #attr} method with the <code>abs:</code> prefix, e.g.:
     * <code>String absUrl = linkEl.attr("abs:href");</code>
     * </p>
     *
     * @param attributeKey The attribute key
     * @return An absolute URL if one could be made, or an empty string (not null) if the attribute was missing or
     * could not be made successfully into a URL.
     * @see #attr
     * @see java.net.URL#URL(java.net.URL, String)
     */
    public String absUrl(String attributeKey) {
        Validate.notEmpty(attributeKey);
        if (!(hasAttributes() && attributes().hasKeyIgnoreCase(attributeKey))) // not using hasAttr, so that we don't recurse down hasAttr->absUrl
            return "";

        return StringUtil.resolve(baseUri(), attributes().getIgnoreCase(attributeKey));
    }
}
