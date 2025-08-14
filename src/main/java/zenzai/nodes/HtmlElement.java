package zenzai.nodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.*;

import zenzai.helper.Validate;
import zenzai.internal.StringUtil;
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
