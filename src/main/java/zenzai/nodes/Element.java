package zenzai.nodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.DOMException;

import zenzai.helper.Validate;
import zenzai.internal.StringUtil;
import zenzai.parser.ParseSettings;
import zenzai.parser.Parser;
import zenzai.parser.Tag;

public abstract class Element extends zenzai.nodes.Node implements org.w3c.dom.Element, Iterable<Element> {
    private static final NodeList EmptyNodeList = new NodeList(0);
    static final String BaseUriKey = Attributes.internalKey("baseUri");
    Tag tag;
    NodeList childNodes;
    @Nullable Attributes attributes; // field is nullable but all methods for attributes are non-null

    public abstract String getTagName();
    public abstract String getAttribute(String name);
    public abstract void setAttribute(String name, String value) throws DOMException;
    public abstract void removeAttribute(String name) throws DOMException;
    public abstract org.w3c.dom.Attr getAttributeNode(String name);
    public abstract org.w3c.dom.Attr setAttributeNode(org.w3c.dom.Attr attr) throws DOMException;
    public abstract org.w3c.dom.Attr removeAttributeNode(org.w3c.dom.Attr attr) throws DOMException;
    public abstract org.w3c.dom.NodeList getElementsByTagName(String name);
    public abstract String getAttributeNS(String namespaceURI, String localName) throws DOMException;
    public abstract void setAttributeNS(String namespaceURI, String qualifiedName, String value) throws DOMException;
    public abstract void removeAttributeNS(String namespaceURI, String localName) throws DOMException;
    public abstract org.w3c.dom.Attr getAttributeNodeNS(String namespaceURI, String localName) throws DOMException;
    public abstract org.w3c.dom.Attr setAttributeNodeNS(org.w3c.dom.Attr attr) throws DOMException;
    public abstract org.w3c.dom.NodeList getElementsByTagNameNS(String namespaceURI, String localName) throws DOMException;
    public abstract boolean hasAttribute(String name);
    public abstract boolean hasAttributeNS(String namespaceURI, String localName) throws DOMException;
    public abstract org.w3c.dom.TypeInfo getSchemaTypeInfo();
    public abstract void setIdAttribute(String name, boolean isId) throws DOMException;
    public abstract void setIdAttributeNS(String namespaceURI, String qualifiedName, boolean isId) throws DOMException;
    public abstract void setIdAttributeNode(org.w3c.dom.Attr idAttr, boolean isId) throws DOMException;

    /**
     * Create a new, standalone Element. (Standalone in that it has no parent.)
     *
     * @param tag tag of this element
     * @param baseUri the base URI (optional, may be null to inherit from parent, or "" to clear parent's)
     * @param attributes initial attributes (optional, may be null)
     * @see #appendChild(Node)
     * @see #appendElement(String)
     */
    public Element(Tag tag, @Nullable String baseUri, @Nullable Attributes attributes) {
        Validate.notNull(tag);
        childNodes = EmptyNodeList;
        this.attributes = attributes;
        this.attributes.setOwnerElement(this);
        this.tag = tag;
        if (!StringUtil.isBlank(baseUri)) this.setBaseUri(baseUri);
    }

    /**
     * Create a new Element from a Tag and a base URI.
     *
     * @param tag element tag
     * @param baseUri the base URI of this element. Optional, and will inherit from its parent, if any.
     * @see Tag#valueOf(String, ParseSettings)
     */
    public Element(Tag tag, @Nullable String baseUri) {
        this(tag, baseUri, null);
    }

    /**
     * Create a new, standalone element, in the specified namespace.
     * @param tag tag name
     * @param namespace namespace for this element
     */
    public Element(String tag, String namespace) {
        this(Tag.valueOf(tag, namespace, ParseSettings.preserveCase), null);
    }

    /**
     * Create a new, standalone element, in the HTML namespace.
     * @param tag tag name
     * @see #Element(String tag, String namespace)
     */
    public Element(String tag) {
        this(tag, Parser.NamespaceHtml);
    }

    /**
     Returns an Iterator that iterates this Element and each of its descendant Elements, in document order.
     @return an Iterator
     */
    @Override
    public Iterator<zenzai.nodes.Element> iterator() {
        return new NodeIterator<>(this, zenzai.nodes.Element.class);
    }

    @Override
    public String getNodeName() {
        return tag.getName();
    }

    @Override
    public int childNodeSize() {
        return childNodes.size();
    }

    @Override
    protected void doSetBaseUri(String baseUri) {
        attributes().put(BaseUriKey, baseUri);
    }

    /**
     * Get the normalized name of this Element's tag. This will always be the lower-cased version of the tag, regardless
     * of the tag case preserving setting of the parser. For e.g., {@code <DIV>} and {@code <div>} both have a
     * normal name of {@code div}.
     * @return normal name
     */
    @Override
    public String normalName() {
        return tag.normalName();
    }

    @Override
    public Element shallowClone() {
        // simpler than implementing a clone version with no child copy
        String baseUri = baseUri();
        if (baseUri.isEmpty()) baseUri = null; // saves setting a blank internal attribute
        return new zenzai.nodes.Element(tag, baseUri, attributes == null ? null : attributes.clone());
    }

    @Override
    public Attributes attributes() {
        if (attributes == null) {// not using hasAttributes, as doesn't clear warning
            attributes = new Attributes();
            attributes.setOwnerElement(this);
        }
        return attributes;
    }

    /**
     * Insert a node to the end of this Element's children. The incoming node will be re-parented.
     *
     * @param child node to add.
     * @return this Element, for chaining
     * @see #prependChild(Node)
     * @see #insertChildren(int, Collection)
     */
    public Element appendChild(Node child) {
        Validate.notNull(child);

        // was - Node#addChildren(child). short-circuits an array create and a loop.
        reparentChild(child);
        ensureChildNodes();
        childNodes.add(child);
        child.setSiblingIndex(childNodes.size() - 1);
        return this;
    }

    /**
     Insert the given nodes to the end of this Element's children.

     @param children nodes to add
     @return this Element, for chaining
     @see #insertChildren(int, Collection)
     */
    public Element appendChildren(Collection<? extends zenzai.nodes.Node> children) {
        insertChildren(-1, children);
        return this;
    }

    /**
     * Inserts the given child nodes into this element at the specified index. Current nodes will be shifted to the
     * right. The inserted nodes will be moved from their current parent. To prevent moving, copy the nodes first.
     *
     * @param index 0-based index to insert children at. Specify {@code 0} to insert at the start, {@code -1} at the
     * end
     * @param children child nodes to insert
     * @return this element, for chaining.
     */
    public Element insertChildren(int index, Collection<? extends zenzai.nodes.Node> children) {
        Validate.notNull(children, "Children collection to be inserted must not be null.");
        int currentSize = childNodeSize();
        if (index < 0) index += currentSize +1; // roll around
        Validate.isTrue(index >= 0 && index <= currentSize, "Insert position out of bounds.");
        addChildren(index, children.toArray(new zenzai.nodes.Node[0]));
        return this;
    }

    /**
     * Inserts the given child nodes into this element at the specified index. Current nodes will be shifted to the
     * right. The inserted nodes will be moved from their current parent. To prevent moving, copy the nodes first.
     *
     * @param index 0-based index to insert children at. Specify {@code 0} to insert at the start, {@code -1} at the
     * end
     * @param children child nodes to insert
     * @return this element, for chaining.
     */
    public Element insertChildren(int index, zenzai.nodes.Node... children) {
        Validate.notNull(children, "Children collection to be inserted must not be null.");
        int currentSize = childNodeSize();
        if (index < 0) index += currentSize +1; // roll around
        Validate.isTrue(index >= 0 && index <= currentSize, "Insert position out of bounds.");

        addChildren(index, children);
        return this;
    }

    /**
     * Add a node to the start of this element's children.
     *
     * @param child node to add.
     * @return this element, so that you can add more child nodes or elements.
     */
    public Element prependChild(Node child) {
        Validate.notNull(child);

        addChildren(0, child);
        return this;
    }

    /**
     * Get the Tag for this element.
     *
     * @return the tag object
     */
    public Tag tag() {
        return tag;
    }

    /**
     * Get the name of the tag for this element. E.g. {@code div}. If you are using {@link ParseSettings#preserveCase
     * case preserving parsing}, this will return the source's original case.
     *
     * @return the tag name
     */
    public String tagName() {
        return tag.getName();
    }

    /**
     Test if this Element has the specified normalized name, and is in the specified namespace.
     * @param normalName a normalized element name (e.g. {@code div}).
     * @param namespace the namespace
     * @return true if the element's normal name matches exactly, and is in the specified namespace
     * @since 1.17.2
     */
    public boolean elementIs(String normalName, String namespace) {
        return tag.normalName().equals(normalName) && tag.namespace().equals(namespace);
    }

    /**
     Get the source range (start and end positions) of the end (closing) tag for this Element. Position tracking must be
     enabled prior to parsing the content.
     @return the range of the closing tag for this element, or {@code untracked} if its range was not tracked.
     @see org.jsoup.parser.Parser#setTrackPosition(boolean)
     @see zenzai.nodes.Node#sourceRange()
     @see Range#isImplicit()
     @since 1.15.2
     */
    public Range endSourceRange() {
        return Range.of(this, false);
    }

    /**
     * Create a new element by tag name, and add it as this Element's last child.
     *
     * @param tagName the name of the tag (e.g. {@code div}).
     * @return the new element, to allow you to add content to it, e.g.:
     *  {@code parent.appendElement("h1").attr("id", "header").text("Welcome");}
     */
    public Element appendElement(String tagName) {
        return appendElement(tagName, tag.namespace());
    }

    /**
     * Create a new element by tag name and namespace, add it as this Element's last child.
     *
     * @param tagName the name of the tag (e.g. {@code div}).
     * @param namespace the namespace of the tag (e.g. {@link Parser#NamespaceHtml})
     * @return the new element, in the specified namespace
     */
    public Element appendElement(String tagName, String namespace) {
        Parser parser = NodeUtils.parser(this);
        Element child = new Element(parser.tagSet().valueOf(tagName, namespace, parser.settings()), baseUri());
        appendChild(child);
        return child;
    }

    /**
     Gets the first child of this Element that is an Element, or {@code null} if there is none.
     @return the first Element child node, or null.
     @see #firstChild()
     @see #lastElementChild()
     @since 1.15.2
     */
    public @Nullable Element firstElementChild() {
        int size = childNodes.size();
        for (int i = 0; i < size; i++) {
            zenzai.nodes.Node node = childNodes.get(i);
            if (node instanceof Element) return (Element) node;
        }
        return null;
    }

    /**
     Gets the last child of this Element that is an Element, or @{code null} if there is none.
     @return the last Element child node, or null.
     @see #lastChild()
     @see #firstElementChild()
     @since 1.15.2
     */
    public @Nullable Element lastElementChild() {
        for (int i = childNodes.size() - 1; i >= 0; i--) {
            Node node = childNodes.get(i);
            if (node instanceof Element) return (Element) node;
        }
        return null;
    }

    @Override @Nullable
    public final Element parent() {
        return (Element) parentNode;
    }

    /**
     * Insert the specified node into the DOM before this node (as a preceding sibling).
     * @param node to add before this element
     * @return this Element, for chaining
     * @see #after(Node)
     */
    @Override
    public Element before(zenzai.nodes.Node node) {
        return (Element) super.before(node);
    }

    void reindexChildren() {
        final int size = childNodes.size();
        for (int i = 0; i < size; i++) {
            childNodes.get(i).setSiblingIndex(i);
        }
        childNodes.validChildren = true;
    }

    void invalidateChildren() {
        childNodes.validChildren = false;
    }

    @Override protected List<zenzai.nodes.Node> ensureChildNodes() {
        if (childNodes == EmptyNodeList) {
            childNodes = new NodeList(4);
        }
        return childNodes;
    }

    static final class NodeList extends ArrayList<zenzai.nodes.Node> implements org.w3c.dom.NodeList {
        /** Tracks if the children have valid sibling indices. We only need to reindex on siblingIndex() demand. */
        boolean validChildren = true;

        @Override
        public org.w3c.dom.Node item(int index) {
            return get(index);
        }

        @Override
        public int getLength() {
            return size();
        }

        public NodeList(int size) {
            super(size);
        }

        int modCount() {
            return this.modCount;
        }
    }
}
