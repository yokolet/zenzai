package zenzai.nodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.*;

import zenzai.helper.Validate;
import zenzai.internal.StringUtil;
import zenzai.parser.HtmlParseSettings;
import zenzai.parser.HtmlParser;
import zenzai.parser.Tag;

public abstract class HtmlElement extends HtmlNode implements Element, Iterable<HtmlElement> {
    private static final HtmlNodeList EmptyNodeList = new HtmlNodeList(0);
    Tag tag;
    HtmlNodeList childNodes;


    public abstract String getTagName();
    public abstract String getAttribute(String name);
    public abstract void setAttribute(String name, String value) throws DOMException;
    public abstract void removeAttribute(String name) throws DOMException;
    public abstract Attr getAttributeNode(String name);
    public abstract Attr setAttributeNode(Attr attr) throws DOMException;
    public abstract Attr removeAttributeNode(Attr attr) throws DOMException;
    public abstract NodeList getElementsByTagName(String name);
    public abstract String getAttributeNS(String namespaceURI, String localName) throws DOMException;
    public abstract void setAttributeNS(String namespaceURI, String qualifiedName, String value) throws DOMException;
    public abstract void removeAttributeNS(String namespaceURI, String localName) throws DOMException;
    public abstract Attr getAttributeNodeNS(String namespaceURI, String localName) throws DOMException;
    public abstract Attr setAttributeNodeNS(Attr attr) throws DOMException;
    public abstract NodeList getElementsByTagNameNS(String namespaceURI, String localName) throws DOMException;
    public abstract boolean hasAttribute(String name);
    public abstract boolean hasAttributeNS(String namespaceURI, String localName) throws DOMException;
    public abstract TypeInfo getSchemaTypeInfo();
    public abstract void setIdAttribute(String name, boolean isId) throws DOMException;
    public abstract void setIdAttributeNS(String namespaceURI, String qualifiedName, boolean isId) throws DOMException;
    public abstract void setIdAttributeNode(Attr idAttr, boolean isId) throws DOMException;

    /**
     * Create a new, standalone Element. (Standalone in that it has no parent.)
     *
     * @param tag tag of this element
     * @param baseUri the base URI (optional, may be null to inherit from parent, or "" to clear parent's)
     * @param attributes initial attributes (optional, may be null)
     * @see #appendChild(Node)
     * @see #appendElement(String)
     */
    public HtmlElement(Tag tag, @Nullable String baseUri, @Nullable Attributes attributes) {
        Validate.notNull(tag);
        childNodes = EmptyNodeList;
        this.attributes = attributes;
        this.tag = tag;
        if (!StringUtil.isBlank(baseUri)) this.setBaseUri(baseUri);
    }


    /**
     * Create a new Element from a Tag and a base URI.
     *
     * @param tag element tag
     * @param baseUri the base URI of this element. Optional, and will inherit from its parent, if any.
     * @see Tag#valueOf(String, HtmlParseSettings)
     */
    public HtmlElement(Tag tag, @Nullable String baseUri) {
        this(tag, baseUri, null);
    }

    /**
     Returns an Iterator that iterates this Element and each of its descendant Elements, in document order.
     @return an Iterator
     */
    @Override
    public abstract Iterator<HtmlElement> iterator();

    /**
     Insert the given nodes to the end of this Element's children.

     @param children nodes to add
     @return this Element, for chaining
     @see #insertChildren(int, Collection)
     */
    public HtmlElement appendChildren(Collection<? extends HtmlNode> children) {
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
    public HtmlElement insertChildren(int index, Collection<? extends HtmlNode> children) {
        Validate.notNull(children, "Children collection to be inserted must not be null.");
        int currentSize = childNodeSize();
        if (index < 0) index += currentSize +1; // roll around
        Validate.isTrue(index >= 0 && index <= currentSize, "Insert position out of bounds.");
        addChildren(index, children.toArray(new Node[0]));
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
    public HtmlElement insertChildren(int index, HtmlNode... children) {
        Validate.notNull(children, "Children collection to be inserted must not be null.");
        int currentSize = childNodeSize();
        if (index < 0) index += currentSize +1; // roll around
        Validate.isTrue(index >= 0 && index <= currentSize, "Insert position out of bounds.");

        addChildren(index, children);
        return this;
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

    /**
     * Get the Tag for this element.
     *
     * @return the tag object
     */
    public Tag tag() {
        return tag;
    }

    /**
     * Get the name of the tag for this element. E.g. {@code div}. If you are using {@link HtmlParseSettings#preserveCase
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
     @see HtmlNode#sourceRange()
     @see HtmlRange#isImplicit()
     @since 1.15.2
     */
    public HtmlRange endSourceRange() {
        return HtmlRange.of(this, false);
    }

    /**
     * Create a new element by tag name, and add it as this Element's last child.
     *
     * @param tagName the name of the tag (e.g. {@code div}).
     * @return the new element, to allow you to add content to it, e.g.:
     *  {@code parent.appendElement("h1").attr("id", "header").text("Welcome");}
     */
    public HtmlElement appendElement(String tagName) {
        return appendElement(tagName, tag.namespace());
    }

    /**
     * Create a new element by tag name and namespace, add it as this Element's last child.
     *
     * @param tagName the name of the tag (e.g. {@code div}).
     * @param namespace the namespace of the tag (e.g. {@link HtmlParser#NamespaceHtml})
     * @return the new element, in the specified namespace
     */
    public HtmlElement appendElement(String tagName, String namespace) {
        HtmlParser parser = NodeUtils.parser(this);
        HtmlElement child = new HtmlElement(parser.tagSet().valueOf(tagName, namespace, parser.settings()), baseUri());
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
    public @Nullable HtmlElement firstElementChild() {
        int size = childNodes.size();
        for (int i = 0; i < size; i++) {
            HtmlNode node = childNodes.get(i);
            if (node instanceof HtmlElement) return (HtmlElement) node;
        }
        return null;
    }

    void reindexChildren() {
        final int size = childNodes.size();
        for (int i = 0; i < size; i++) {
            childNodes.get(i).setSiblingIndex(i);
        }
        childNodes.validChildren = true;
    }

    @Override protected List<HtmlNode> ensureChildNodes() {
        if (childNodes == EmptyNodeList) {
            childNodes = new HtmlNodeList(4);
        }
        return childNodes;
    }

    static final class HtmlNodeList extends ArrayList<HtmlNode> {
        /** Tracks if the children have valid sibling indices. We only need to reindex on siblingIndex() demand. */
        boolean validChildren = true;

        public HtmlNodeList(int size) {
            super(size);
        }

        int modCount() {
            return this.modCount;
        }
    }
}
