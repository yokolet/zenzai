package nokogiri.internals.html.nodes;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;

import nokogiri.internals.html.helper.Validate;
import nokogiri.internals.html.helper.W3CValidation;
import nokogiri.internals.html.internal.Normalizer;
import nokogiri.internals.html.internal.QuietAppendable;
import nokogiri.internals.html.internal.StringUtil;
import nokogiri.internals.html.parser.ParseSettings;
import nokogiri.internals.html.parser.Parser;
import nokogiri.internals.html.parser.Tag;
import nokogiri.internals.html.select.Collector;
import nokogiri.internals.html.select.Elements;
import nokogiri.internals.html.select.Evaluator;
import nokogiri.internals.html.select.NodeFilter;
import nokogiri.internals.html.select.NodeVisitor;
import nokogiri.internals.html.select.Selector;

import static nokogiri.internals.html.nodes.Document.OutputSettings.Syntax.xml;
import static nokogiri.internals.html.parser.Parser.NamespaceHtml;
import static nokogiri.internals.html.nodes.TextNode.lastCharIsWhitespace;

public class Element extends nokogiri.internals.html.nodes.Node implements org.w3c.dom.Element, Iterable<Element> {
    private static final List<Element> EmptyChildren = Collections.emptyList();
    private static final NodeList EmptyNodeList = new NodeList(0);
    static final String BaseUriKey = Attributes.internalKey("baseUri");
    Tag tag;
    NodeList childNodes;
    @Nullable Attributes attributes; // field is nullable but all methods for attributes are non-null

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
        if (this.attributes != null) {
            this.attributes.setOwnerElement(this);
        }
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
    public Iterator<nokogiri.internals.html.nodes.Element> iterator() {
        return new NodeIterator<>(this, nokogiri.internals.html.nodes.Element.class);
    }

    // org.w3c.dom.Node
    @Override
    public String getNodeName() {
        return tag.getName();
    }

    // org.w3c.dom.Node
    @Override
    public short getNodeType() {
        return Node.ELEMENT_NODE;
    }

    // org.w3c.dom.Node
    @Override
    public org.w3c.dom.NodeList getChildNodes() {
        return childNodes;
    }

    // org.w3c.dom.Node
    @Override
    public org.w3c.dom.Node getFirstChild() {
        if (childNodes == null || childNodes.getLength() == 0) return null;
        return childNodes.item(0);
    }

    // org.w3c.dom.Node
    @Override
    public org.w3c.dom.Node getLastChild() {
        if (childNodes == null || childNodes.getLength() == 0) return null;
        return childNodes.item(childNodes.getLength() - 1);
    }

    // org.w3c.dom.Node
    @Override
    public NamedNodeMap getAttributes() { return attributes(); }

    // org.w3c.dom.Node
    @Override
    public org.w3c.dom.Node insertBefore(org.w3c.dom.Node newChild, org.w3c.dom.Node refChild) throws DOMException {
        W3CValidation.hierarchyRequest(this, (nokogiri.internals.html.nodes.Node)newChild);
        W3CValidation.wrongDocument(this, (nokogiri.internals.html.nodes.Node)newChild);
        W3CValidation.modificationAllowed(this, (nokogiri.internals.html.nodes.Node)newChild);

        if (refChild != null) {
            W3CValidation.nodeInChildren(this, (nokogiri.internals.html.nodes.Node)refChild);
            ((nokogiri.internals.html.nodes.Node)refChild).before((nokogiri.internals.html.nodes.Node)newChild);
        } else {
            addChildren((nokogiri.internals.html.nodes.Node)newChild);
        }
        return newChild;
    }

    // org.w3c.dom.Node
    // nokogiri.internals.html.nodes.Node
    @Override
    public boolean hasAttributes() {
        return attributes != null;
    }

    // org.w3c.dom.Node
    /**
     Internal test to check if a nodelist object has been created.
     */
    @Override
    public boolean hasChildNodes() {
        return childNodes != EmptyNodeList;
    }

    // org.w3c.dom.Node
    @Override
    public String getTextContent() throws DOMException {
        // TODO: check if calling text() is enough
        // concatenation of the textContent attribute value of every child node, excluding COMMENT_NODE and
        // PROCESSING_INSTRUCTION_NODE nodes. This is the empty string if the node has no children.
        return text();
    }

    // org.w3c.dom.Node
    @Override
    public void setTextContent(String textContent) throws DOMException {
        text(textContent);
    }

    // org.w3c.dom.Element
    @Override
    public String getTagName() { return tagName(); }

    // org.w3c.dom.Element
    @Override
    public String getAttribute(String name) {
        Attribute attr = attribute(name);
        return attr == null ? null : attr.getValue();
    }

    // org.w3c.dom.Element
    @Override
    public void setAttribute(String name, String value) throws DOMException {
        W3CValidation.modificationAllowed(this);
        attr(name, value);
    }

    // org.w3c.dom.Element
    public void removeAttribute(String name) throws DOMException {
        W3CValidation.modificationAllowed(this);
        removeAttr(name);
    }

    // org.w3c.dom.Element
    @Override
    public org.w3c.dom.Attr getAttributeNode(String name) {
        return attribute(name);
    }

    // org.w3c.dom.Element
    @Override
    public org.w3c.dom.Attr setAttributeNode(org.w3c.dom.Attr attr) throws DOMException {
        if (getOwnerDocument() != attr.getOwnerDocument()) {
            throw new DOMException(DOMException.WRONG_DOCUMENT_ERR, "Cannot set attribute on non-owner document.");
        }
        W3CValidation.modificationAllowed(this);
        if (this != ((Attribute)attr).getOwnerDocument()) {
            throw new DOMException(DOMException.INUSE_ATTRIBUTE_ERR, "Cannot set attribute on non-owner document.");
        }
        attributes().put((Attribute)attr);
        return attr;
    }

    // org.w3c.dom.Element
    @Override
    public org.w3c.dom.Attr removeAttributeNode(org.w3c.dom.Attr attr) throws DOMException {
        W3CValidation.modificationAllowed(this);
        String key = attr.getName();
        if (!attributes().hasKey(key)) {
            throw new DOMException(DOMException.NOT_FOUND_ERR, "Cannot remove attribute on non-owner document.");
        }
        attributes().remove(key);
        return attr;
    }

    // org.w3c.dom.Element
    @Override
    public org.w3c.dom.NodeList getElementsByTagName(String name) {
        return getElementsByTag(name);
    }

    // org.w3c.dom.Element
    @Override
    public boolean hasAttribute(String name) {
        return attributes().hasKey(name);
    }

    // org.w3c.dom.Element
    @Override
    public org.w3c.dom.TypeInfo getSchemaTypeInfo() {
        return null;
    }

    // org.w3c.dom.Element
    public String getAttributeNS(String namespaceURI, String localName) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported operation in HTML");
    }
    public void setAttributeNS(String namespaceURI, String qualifiedName, String value) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported operation in HTML");
    }
    public void removeAttributeNS(String namespaceURI, String localName) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported operation in HTML");
    }
    public org.w3c.dom.Attr getAttributeNodeNS(String namespaceURI, String localName) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported operation in HTML");
    }
    public org.w3c.dom.Attr setAttributeNodeNS(org.w3c.dom.Attr attr) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported operation in HTML");
    }
    public org.w3c.dom.NodeList getElementsByTagNameNS(String namespaceURI, String localName) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported operation in HTML");
    }
    public boolean hasAttributeNS(String namespaceURI, String localName) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported operation in HTML");
    }
    public void setIdAttribute(String name, boolean isId) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported operation in HTML");
    }
    public void setIdAttributeNS(String namespaceURI, String qualifiedName, boolean isId) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported operation in HTML");
    }
    public void setIdAttributeNode(org.w3c.dom.Attr idAttr, boolean isId) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported operation in HTML");
    }

    @Override
    public int childNodeSize() {
        return childNodes.size();
    }

    @Override
    public String nodeName() {
        return tag.getName();
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
    public Element clone() {
        return (Element) super.clone();
    }

    @Override
    public Element shallowClone() {
        // simpler than implementing a clone version with no child copy
        String baseUri = baseUri();
        if (baseUri.isEmpty()) baseUri = null; // saves setting a blank internal attribute
        return new Element(tag, baseUri, attributes == null ? null : attributes.clone());
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
     * Find all elements under this element (including self, and children of children).
     *
     * @return all elements
     */
    public Elements getAllElements() {
        return Collector.collect(new Evaluator.AllElements(), this);
    }

    @Override
    public String baseUri() {
        String baseUri = searchUpForAttribute(this, BaseUriKey);
        return baseUri != null ? baseUri : "";
    }

    @Override
    public <T extends Appendable> T html(T accum) {
        Node child = firstChild();
        if (child != null) {
            Printer printer = Printer.printerFor(child, QuietAppendable.wrap(accum));
            while (child != null) {
                printer.traverse(child);
                child = child.nextSibling();
            }
        }
        return accum;
    }

    @Override @Nullable
    public final Element parent() {
        return (Element) parentNode;
    }

    /**
     * Insert the specified HTML into the DOM before this element (as a preceding sibling).
     *
     * @param html HTML to add before this element
     * @return this element, for chaining
     * @see #after(String)
     */
    @Override
    public Element before(String html) {
        return (Element) super.before(html);
    }

    /**
     * Insert the specified node into the DOM before this node (as a preceding sibling).
     * @param node to add before this element
     * @return this Element, for chaining
     * @see #after(Node)
     */
    @Override
    public Element before(Node node) {
        return (Element) super.before(node);
    }

    /**
     * Insert the specified HTML into the DOM after this element (as a following sibling).
     *
     * @param html HTML to add after this element
     * @return this element, for chaining
     * @see #before(String)
     */
    @Override
    public Element after(String html) {
        return (Element) super.after(html);
    }

    /**
     * Insert the specified node into the DOM after this node (as a following sibling).
     * @param node to add after this element
     * @return this element, for chaining
     * @see #before(Node)
     */
    @Override
    public Element after(Node node) {
        return (Element) super.after(node);
    }

    /**
     * Remove all the element's child nodes. Any attributes are left as-is. Each child node has its parent set to
     * {@code null}.
     * @return this element
     */
    @Override
    public Element empty() {
        // Detach each of the children -> parent links:
        int size = childNodes.size();
        for (int i = 0; i < size; i++)
            childNodes.get(i).parentNode = null;
        childNodes.clear();
        return this;
    }

    /**
     * Wrap the supplied HTML around this element.
     *
     * @param html HTML to wrap around this element, e.g. {@code <div class="head"></div>}. Can be arbitrarily deep.
     * @return this element, for chaining.
     */
    @Override
    public Element wrap(String html) {
        return (Element) super.wrap(html);
    }

    /**
     * Set an attribute value on this element. If this element already has an attribute with the
     * key, its value is updated; otherwise, a new attribute is added.
     *
     * @return this element
     */
    @Override public Element attr(String attributeKey, String attributeValue) {
        super.attr(attributeKey, attributeValue);
        return this;
    }

    // overrides of Node for call chaining
    @Override
    public Element clearAttributes() {
        if (attributes != null) {
            super.clearAttributes(); // keeps internal attributes via iterator
            if (attributes.size == 0)
                attributes = null; // only remove entirely if no internal attributes
        }

        return this;
    }

    @Override
    public Element removeAttr(String attributeKey) {
        return (Element) super.removeAttr(attributeKey);
    }

    @Override
    public Element root() {
        return (Element) super.root(); // probably a document, but always at least an element
    }

    @Override
    public Element traverse(NodeVisitor nodeVisitor) {
        return (Element) super.traverse(nodeVisitor);
    }

    @Override
    public Element forEachNode(Consumer<? super Node> action) {
        return (Element) super.forEachNode(action);
    }

    /**
     Perform the supplied action on this Element and each of its descendant Elements, during a depth-first traversal.
     Elements may be inspected, changed, added, replaced, or removed.
     @param action the function to perform on the element
     @see Node#forEachNode(Consumer)
     */
    @Override
    public void forEach(Consumer<? super Element> action) {
        stream().forEach(action);
    }

    /**
     * Retrieves the element's inner HTML. E.g. on a {@code <div>} with one empty {@code <p>}, would return
     * {@code <p></p>}. (Whereas {@link #outerHtml()} would return {@code <div><p></p></div>}.)
     *
     * @return String of HTML.
     * @see #outerHtml()
     */
    public String html() {
        StringBuilder sb = StringUtil.borrowBuilder();
        html(sb);
        String html = StringUtil.releaseBuilder(sb);
        return NodeUtils.outputSettings(this).prettyPrint() ? html.trim() : html;
    }

    /**
     * Set this element's inner HTML. Clears the existing HTML first.
     * @param html HTML to parse and set into this element
     * @return this element
     * @see #append(String)
     */
    public Element html(String html) {
        empty();
        append(html);
        return this;
    }

    /**
     * Set a boolean attribute value on this element. Setting to <code>true</code> sets the attribute value to "" and
     * marks the attribute as boolean so no value is written out. Setting to <code>false</code> removes the attribute
     * with the same key if it exists.
     *
     * @param attributeKey the attribute key
     * @param attributeValue the attribute value
     *
     * @return this element
     */
    public Element attr(String attributeKey, boolean attributeValue) {
        attributes().put(attributeKey, attributeValue);
        return this;
    }

    /**
     * Get this element's parent and ancestors, up to the document root.
     * @return this element's stack of parents, starting with the closest first.
     */
    public Elements parents() {
        Elements parents = new Elements();
        Element parent = this.parent();
        while (parent != null && !parent.nameIs("#root")) {
            parents.add(parent);
            parent = parent.parent();
        }
        return parents;
    }

    /**
     * Get a child element of this element, by its 0-based index number.
     * <p>
     * Note that an element can have both mixed Nodes and Elements as children. This method inspects
     * a filtered list of children that are elements, and the index is based on that filtered list.
     * </p>
     *
     * @param index the index number of the element to retrieve
     * @return the child element, if it exists, otherwise throws an {@code IndexOutOfBoundsException}
     * @see #childNode(int)
     */
    public Element child(int index) {
        Validate.isTrue(index >= 0, "Index must be >= 0");
        List<Element> cached = cachedChildren();
        if (cached != null) return cached.get(index);
        // otherwise, iter on elementChild; saves creating list
        int size = childNodes.size();
        for (int i = 0, e = 0; i < size; i++) { // direct iter is faster than chasing firstElSib, nextElSibd
            Node node = childNodes.get(i);
            if (node instanceof Element) {
                if (e++ == index) return (Element) node;
            }
        }
        throw new IndexOutOfBoundsException("No child at index: " + index);
    }

    /**
     * Get the number of child nodes of this element that are elements.
     * <p>
     * This method works on the same filtered list like {@link #child(int)}. Use {@link #childNodes()} and {@link
     * #childNodeSize()} to get the unfiltered Nodes (e.g. includes TextNodes etc.)
     * </p>
     *
     * @return the number of child nodes that are elements
     * @see #children()
     * @see #child(int)
     */
    public int childrenSize() {
        if (childNodeSize() == 0) return 0;
        return childElementsList().size(); // gets children into cache; faster subsequent child(i) if unmodified
    }

    /**
     * Get this element's child elements.
     * <p>
     * This is effectively a filter on {@link #childNodes()} to get Element nodes.
     * </p>
     * @return child elements. If this element has no children, returns an empty list.
     * @see #childNodes()
     */
    public Elements children() {
        return new Elements(childElementsList());
    }

    /**
     Get an Attribute by key. Changes made via {@link Attribute#setKey(String)}, {@link Attribute#setValue(String)} etc
     will cascade back to this Element.
     @param key the (case-sensitive) attribute key
     @return the Attribute for this key, or null if not present.
     @since 1.17.2
     */
    @Nullable public Attribute attribute(String key) {
        return hasAttributes() ? attributes().attribute(key) : null;
    }

    /**
     * Get this element's HTML5 custom data attributes. Each attribute in the element that has a key
     * starting with "data-" is included the dataset.
     * <p>
     * E.g., the element {@code <div data-package="jsoup" data-language="Java" class="group">...} has the dataset
     * {@code package=jsoup, language=java}.
     * <p>
     * This map is a filtered view of the element's attribute map. Changes to one map (add, remove, update) are reflected
     * in the other map.
     * <p>
     * You can find elements that have data attributes using the {@code [^data-]} attribute key prefix selector.
     * @return a map of {@code key=value} custom data attributes.
     */
    public Map<String, String> dataset() {
        return attributes().dataset();
    }

    /**
     Gets the <b>normalized, combined text</b> of this element and all its children. Whitespace is normalized and
     trimmed.
     <p>For example, given HTML {@code <p>Hello  <b>there</b> now! </p>}, {@code p.text()} returns {@code "Hello there
    now!"}
     <p>If you do not want normalized text, use {@link #wholeText()}. If you want just the text of this node (and not
     children), use {@link #ownText()}
     <p>Note that this method returns the textual content that would be presented to a reader. The contents of data
     nodes (such as {@code <script>} tags) are not considered text. Use {@link #data()} or {@link #html()} to retrieve
     that content.

     @return decoded, normalized text, or empty string if none.
     @see #wholeText()
     @see #ownText()
     @see #textNodes()
     */
    public String text() {
        final StringBuilder accum = StringUtil.borrowBuilder();
        new TextAccumulator(accum).traverse(this);
        return StringUtil.releaseBuilder(accum).trim();
    }

    /**
     * Set the text of this element. Any existing contents (text or elements) will be cleared.
     * <p>As a special case, for {@code <script>} and {@code <style>} tags, the input text will be treated as data,
     * not visible text.</p>
     * @param text decoded text
     * @return this element
     */
    public Element text(String text) {
        Validate.notNull(text);
        empty();
        // special case for script/style in HTML (or customs): should be data node
        if (tag().is(Tag.Data))
            appendChild(new DataNode(text));
        else
            appendChild(new TextNode(text));

        return this;
    }

    /**
     Get the non-normalized, decoded text of this element and its children, including only any newlines and spaces
     present in the original source.
     @return decoded, non-normalized text
     @see #text()
     @see #wholeOwnText()
     */
    public String wholeText() {
        return wholeTextOf(nodeStream());
    }

    /**
     An Element's nodeValue is its whole own text.
     */
    @Override
    public String nodeValue() {
        return wholeOwnText();
    }

    /**
     Get the non-normalized, decoded text of this element, <b>not including</b> any child elements, including any
     newlines and spaces present in the original source.
     @return decoded, non-normalized text that is a direct child of this Element
     @see #text()
     @see #wholeText()
     @see #ownText()
     @since 1.15.1
     */
    public String wholeOwnText() {
        return wholeTextOf(childNodes.stream());
    }

    /**
     * Gets the (normalized) text owned by this element only; does not get the combined text of all children.
     * <p>
     * For example, given HTML {@code <p>Hello <b>there</b> now!</p>}, {@code p.ownText()} returns {@code "Hello now!"},
     * whereas {@code p.text()} returns {@code "Hello there now!"}.
     * Note that the text within the {@code b} element is not returned, as it is not a direct child of the {@code p} element.
     *
     * @return decoded text, or empty string if none.
     * @see #text()
     * @see #textNodes()
     */
    public String ownText() {
        StringBuilder sb = StringUtil.borrowBuilder();
        ownText(sb);
        return StringUtil.releaseBuilder(sb).trim();
    }

    /**
     Checks if the current element or any of its child elements contain non-whitespace text.
     @return {@code true} if the element has non-blank text content, {@code false} otherwise.
     */
    public boolean hasText() {
        AtomicBoolean hasText = new AtomicBoolean(false);
        filter((node, depth) -> {
            if (node instanceof TextNode) {
                TextNode textNode = (TextNode) node;
                if (!textNode.isBlank()) {
                    hasText.set(true);
                    return NodeFilter.FilterResult.STOP;
                }
            }
            return NodeFilter.FilterResult.CONTINUE;
        });
        return hasText.get();
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
    public Element appendChildren(Collection<? extends nokogiri.internals.html.nodes.Node> children) {
        insertChildren(-1, children);
        return this;
    }

    /**
     * Add this element to the supplied parent element, as its next child.
     *
     * @param parent element to which this element will be appended
     * @return this element, so that you can continue modifying the element
     */
    public Element appendTo(Element parent) {
        Validate.notNull(parent);
        parent.appendChild(this);
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
    public Element insertChildren(int index, Collection<? extends nokogiri.internals.html.nodes.Node> children) {
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
    public Element insertChildren(int index, nokogiri.internals.html.nodes.Node... children) {
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
     Insert the given nodes to the start of this Element's children.

     @param children nodes to add
     @return this Element, for chaining
     @see #insertChildren(int, Collection)
     */
    public Element prependChildren(Collection<? extends Node> children) {
        insertChildren(0, children);
        return this;
    }

    /**
     * Create a new element by tag name, and add it as this Element's first child.
     *
     * @param tagName the name of the tag (e.g. {@code div}).
     * @return the new element, to allow you to add content to it, e.g.:
     *  {@code parent.prependElement("h1").attr("id", "header").text("Welcome");}
     */
    public Element prependElement(String tagName) {
        return prependElement(tagName, tag.namespace());
    }

    /**
     * Create a new element by tag name and namespace, and add it as this Element's first child.
     *
     * @param tagName the name of the tag (e.g. {@code div}).
     * @param namespace the namespace of the tag (e.g. {@link Parser#NamespaceHtml})
     * @return the new element, in the specified namespace
     */
    public Element prependElement(String tagName, String namespace) {
        Parser parser = NodeUtils.parser(this);
        Element child = new Element(parser.tagSet().valueOf(tagName, namespace, parser.settings()), baseUri());
        prependChild(child);
        return child;
    }

    /**
     * Create and append a new TextNode to this element.
     *
     * @param text the (un-encoded) text to add
     * @return this element
     */
    public Element appendText(String text) {
        Validate.notNull(text);
        TextNode node = new TextNode(text);
        appendChild(node);
        return this;
    }

    /**
     * Create and prepend a new TextNode to this element.
     *
     * @param text the decoded text to add
     * @return this element
     */
    public Element prependText(String text) {
        Validate.notNull(text);
        TextNode node = new TextNode(text);
        prependChild(node);
        return this;
    }

    /**
     * Add inner HTML to this element. The supplied HTML will be parsed, and each node appended to the end of the children.
     * @param html HTML to add inside this element, after the existing HTML
     * @return this element
     * @see #html(String)
     */
    public Element append(String html) {
        Validate.notNull(html);
        List<Node> nodes = NodeUtils.parser(this).parseFragmentInput(html, this, baseUri());
        addChildren(nodes.toArray(new Node[0]));
        return this;
    }

    /**
     * Add inner HTML into this element. The supplied HTML will be parsed, and each node prepended to the start of the element's children.
     * @param html HTML to add inside this element, before the existing HTML
     * @return this element
     * @see #html(String)
     */
    public Element prepend(String html) {
        Validate.notNull(html);
        List<Node> nodes = NodeUtils.parser(this).parseFragmentInput(html, this, baseUri());
        addChildren(0, nodes.toArray(new Node[0]));
        return this;
    }

    /**
     * Get sibling elements. If the element has no sibling elements, returns an empty list. An element is not a sibling
     * of itself, so will not be included in the returned list.
     * @return sibling elements
     */
    public Elements siblingElements() {
        if (parentNode == null)
            return new Elements(0);

        List<Element> elements = parent().childElementsList();
        Elements siblings = new Elements(elements.size() - 1);
        for (Element el: elements)
            if (el != this)
                siblings.add(el);
        return siblings;
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
     Change the Tag of this element.
     @param tag the new tag
     @return this element, for chaining
     @since 1.20.1
     */
    public Element tag(Tag tag) {
        Validate.notNull(tag);
        this.tag = tag;
        return this;
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
     * Change (rename) the tag of this element. For example, convert a {@code <span>} to a {@code <div>} with
     * {@code el.tagName("div");}.
     *
     * @param tagName new tag name for this element
     * @return this element, for chaining
     * @see nokogiri.internals.html.select.Elements#tagName(String)
     */
    public Element tagName(String tagName) {
        return tagName(tagName, tag.namespace());
    }

    /**
     * Change (rename) the tag of this element. For example, convert a {@code <span>} to a {@code <div>} with
     * {@code el.tagName("div");}.
     *
     * @param tagName new tag name for this element
     * @param namespace the new namespace for this element
     * @return this element, for chaining
     * @see nokogiri.internals.html.select.Elements#tagName(String)
     */
    public Element tagName(String tagName, String namespace) {
        Validate.notEmptyParam(tagName, "tagName");
        Validate.notEmptyParam(namespace, "namespace");
        Parser parser = NodeUtils.parser(this);
        tag = parser.tagSet().valueOf(tagName, namespace, parser.settings()); // maintains the case option of the original parse
        return this;
    }

    /**
     * Finds elements, including and recursively under this element, with the specified tag name.
     * @param tagName The tag name to search for (case insensitively).
     * @return a matching unmodifiable list of elements. Will be empty if this element and none of its children match.
     */
    public Elements getElementsByTag(String tagName) {
        Validate.notEmpty(tagName);
        tagName = Normalizer.normalize(tagName);

        return Collector.collect(new Evaluator.Tag(tagName), this);
    }

    /**
     * Find an element by ID, including or under this element.
     * <p>
     * Note that this finds the first matching ID, starting with this element. If you search down from a different
     * starting point, it is possible to find a different element by ID. For unique element by ID within a Document,
     * use {@link Document#getElementById(String)}
     * @param id The ID to search for.
     * @return The first matching element by ID, starting with this element, or null if none found.
     */
    public Element getElementById(String id) {
        Validate.notEmpty(id);
        return Collector.findFirst(new Evaluator.Id(id), this);
    }

    /**
     Get the source range (start and end positions) of the end (closing) tag for this Element. Position tracking must be
     enabled prior to parsing the content.
     @return the range of the closing tag for this element, or {@code untracked} if its range was not tracked.
     @see nokogiri.internals.html.parser.Parser#setTrackPosition(boolean)
     @see Node#sourceRange()
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
            Node node = childNodes.get(i);
            if (node instanceof Element) return (Element) node;
        }
        return null;
    }

    /**
     * Gets the first Element sibling of this element. That may be this element.
     * @return the first sibling that is an element (aka the parent's first element child)
     */
    public Element firstElementSibling() {
        if (parent() != null) {
            //noinspection DataFlowIssue (not nullable, would be this is no other sibs)
            return parent().firstElementChild();
        } else
            return this; // orphan is its own first sibling
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

    /**
     * Gets the last element sibling of this element. That may be this element.
     * @return the last sibling that is an element (aka the parent's last element child)
     */
    public Element lastElementSibling() {
        if (parent() != null) {
            //noinspection DataFlowIssue (not nullable, would be this if no other sibs)
            return parent().lastElementChild();
        } else
            return this;
    }

    /**
     * Get the list index of this element in its element sibling list. I.e. if this is the first element
     * sibling, returns 0.
     * @return position in element sibling list
     */
    public int elementSiblingIndex() {
        if (parent() == null) return 0;
        return indexInList(this, parent().childElementsList());
    }

    /**
     * Test if this element is a block-level element. (E.g. {@code <div> == true} or an inline element
     * {@code <span> == false}).
     *
     * @return true if block, false if not (and thus inline)
     */
    public boolean isBlock() {
        return tag.isBlock();
    }

    /**
     * Get the {@code id} attribute of this element.
     *
     * @return The id attribute, if present, or an empty string if not.
     */
    public String id() {
        return attributes != null ? attributes.getIgnoreCase("id") :"";
    }

    /**
     Set the {@code id} attribute of this element.
     @param id the ID value to use
     @return this Element, for chaining
     */
    public Element id(String id) {
        Validate.notNull(id);
        attr("id", id);
        return this;
    }

    /**
     Returns a Stream of this Element and all of its descendant Elements. The stream has document order.
     @return a stream of this element and its descendants.
     @see #nodeStream()
     @since 1.17.1
     */
    public Stream<Element> stream() {
        return NodeUtils.stream(this, Element.class);
    }

    /**
     * Get this element's child text nodes. The list is unmodifiable but the text nodes may be manipulated.
     * <p>
     * This is effectively a filter on {@link #childNodes()} to get Text nodes.
     * @return child text nodes. If this element has no text nodes, returns an
     * empty list.
     * </p>
     * For example, with the input HTML: {@code <p>One <span>Two</span> Three <br> Four</p>} with the {@code p} element selected:
     * <ul>
     *     <li>{@code p.text()} = {@code "One Two Three Four"}</li>
     *     <li>{@code p.ownText()} = {@code "One Three Four"}</li>
     *     <li>{@code p.children()} = {@code Elements[<span>, <br>]}</li>
     *     <li>{@code p.childNodes()} = {@code List<Node>["One ", <span>, " Three ", <br>, " Four"]}</li>
     *     <li>{@code p.textNodes()} = {@code List<TextNode>["One ", " Three ", " Four"]}</li>
     * </ul>
     */
    public List<TextNode> textNodes() {
        return filterNodes(TextNode.class);
    }

    /**
     * Get this element's child data nodes. The list is unmodifiable but the data nodes may be manipulated.
     * <p>
     * This is effectively a filter on {@link #childNodes()} to get Data nodes.
     * </p>
     * @return child data nodes. If this element has no data nodes, returns an
     * empty list.
     * @see #data()
     */
    public List<DataNode> dataNodes() {
        return filterNodes(DataNode.class);
    }

    /**
     * Get the combined data of this element. Data is e.g. the inside of a {@code <script>} tag. Note that data is NOT the
     * text of the element. Use {@link #text()} to get the text that would be visible to a user, and {@code data()}
     * for the contents of scripts, comments, CSS styles, etc.
     *
     * @return the data, or empty string if none
     *
     * @see #dataNodes()
     */
    public String data() {
        StringBuilder sb = StringUtil.borrowBuilder();
        traverse((childNode, depth) -> {
            if (childNode instanceof DataNode) {
                DataNode data = (DataNode) childNode;
                sb.append(data.getWholeData());
            } else if (childNode instanceof Comment) {
                Comment comment = (Comment) childNode;
                sb.append(comment.getData());
            } else if (childNode instanceof CDataNode) {
                // this shouldn't really happen because the html parser won't see the cdata as anything special when parsing script.
                // but in case another type gets through.
                CDataNode cDataNode = (CDataNode) childNode;
                sb.append(cDataNode.getWholeText());
            }
        });
        return StringUtil.releaseBuilder(sb);
    }

    /**
     * Find the first Element that matches the {@link Selector} CSS query, with this element as the starting context.
     * <p>This is effectively the same as calling {@code element.select(query).first()}, but is more efficient as query
     * execution stops on the first hit.</p>
     * <p>Also known as {@code querySelector()} in the Web DOM.</p>
     * @param cssQuery cssQuery a {@link Selector} CSS-like query
     * @return the first matching element, or <b>{@code null}</b> if there is no match.
     * @see #expectFirst(String)
     */
    public @Nullable Element selectFirst(String cssQuery) {
        return Selector.selectFirst(cssQuery, this);
    }

    /**
     * Finds the first Element that matches the supplied Evaluator, with this element as the starting context, or
     * {@code null} if none match.
     *
     * @param evaluator an element evaluator
     * @return the first matching element (walking down the tree, starting from this element), or {@code null} if none
     * match.
     */
    public @Nullable Element selectFirst(Evaluator evaluator) {
        return Collector.findFirst(evaluator, this);
    }

    /**
     Just like {@link #selectFirst(String)}, but if there is no match, throws an {@link IllegalArgumentException}. This
     is useful if you want to simply abort processing on a failed match.
     @param cssQuery a {@link nokogiri.internals.html.select.Selector} CSS-like query
     @return the first matching element
     @throws IllegalArgumentException if no match is found
     @since 1.15.2
     */
    public Element expectFirst(String cssQuery) {
        return Validate.expectNotNull(
                Selector.selectFirst(cssQuery, this),
                parent() != null ?
                        "No elements matched the query '%s' on element '%s'." :
                        "No elements matched the query '%s' in the document."
                , cssQuery, this.tagName()
        );
    }

    public Elements select(String cssQuery) {
        return Selector.select(cssQuery, this);
    }

    /**
     * Find elements that match the supplied Evaluator. This has the same functionality as {@link #select(String)}, but
     * may be useful if you are running the same query many times (on many documents) and want to save the overhead of
     * repeatedly parsing the CSS query.
     * @param evaluator an element evaluator
     * @return an {@link nokogiri.internals.html.select.Elements} list containing elements that match the query (empty if none match)
     * @see nokogiri.internals.html.select.Selector#evaluatorOf(String css)
     */
    public Elements select(Evaluator evaluator) {
        return Selector.select(evaluator, this);
    }

    /**
     * Tests if this element has a class. Case-insensitive.
     * @param className name of class to check for
     * @return true if it does, false if not
     */
    // performance sensitive
    public boolean hasClass(String className) {
        if (attributes == null)
            return false;

        final String classAttr = attributes.getIgnoreCase("class");
        final int len = classAttr.length();
        final int wantLen = className.length();

        if (len == 0 || len < wantLen) {
            return false;
        }

        // if both lengths are equal, only need compare the className with the attribute
        if (len == wantLen) {
            return className.equalsIgnoreCase(classAttr);
        }

        // otherwise, scan for whitespace and compare regions (with no string or arraylist allocations)
        boolean inClass = false;
        int start = 0;
        for (int i = 0; i < len; i++) {
            if (Character.isWhitespace(classAttr.charAt(i))) {
                if (inClass) {
                    // white space ends a class name, compare it with the requested one, ignore case
                    if (i - start == wantLen && classAttr.regionMatches(true, start, className, 0, wantLen)) {
                        return true;
                    }
                    inClass = false;
                }
            } else {
                if (!inClass) {
                    // we're in a class name : keep the start of the substring
                    inClass = true;
                    start = i;
                }
            }
        }

        // check the last entry
        if (inClass && len - start == wantLen) {
            return classAttr.regionMatches(true, start, className, 0, wantLen);
        }

        return false;
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

    boolean hasValidChildren() {
        return childNodes.validChildren;
    }

    @Override
    void outerHtmlHead(final QuietAppendable accum, Document.OutputSettings out) {
        String tagName = safeTagName(out.syntax());
        accum.append('<').append(tagName);
        if (attributes != null) attributes.html(accum, out);

        if (childNodes.isEmpty()) {
            boolean xmlMode = out.syntax() == xml || !tag.namespace().equals(NamespaceHtml);
            if (xmlMode && (tag.is(Tag.SeenSelfClose) || (tag.isKnownTag() && (tag.isEmpty() || tag.isSelfClosing())))) {
                accum.append(" />");
            } else if (!xmlMode && tag.isEmpty()) { // html void element
                accum.append('>');
            } else {
                accum.append("></").append(tagName).append('>');
            }
        } else {
            accum.append('>');
        }
    }

    @Override
    void outerHtmlTail(QuietAppendable accum, Document.OutputSettings out) {
        if (!childNodes.isEmpty())
            accum.append("</").append(safeTagName(out.syntax())).append('>');
        // if empty, we have already closed in htmlHead
    }

    static final class NodeList extends ArrayList<nokogiri.internals.html.nodes.Node> implements org.w3c.dom.NodeList {
        /** Tracks if the children have valid sibling indices. We only need to reindex on siblingIndex() demand. */
        boolean validChildren = true;

        public NodeList(int size) {
            super(size);
        }

        // org.w3c.dom.NodeList
        @Override
        public org.w3c.dom.Node item(int index) {
            return get(index);
        }

        // org.w3c.dom.NodeList
        @Override
        public int getLength() {
            return size();
        }

        int modCount() {
            return this.modCount;
        }

        void incrementMod() {
            this.modCount++;
        }
    }

    @Nullable
    static String searchUpForAttribute(final Element start, final String key) {
        Element el = start;
        while (el != null) {
            if (el.attributes != null && el.attributes.hasKey(key))
                return el.attributes.get(key);
            el = el.parent();
        }
        return null;
    }

    private static final String childElsKey = "jsoup.childEls";
    private static final String childElsMod = "jsoup.childElsMod";

    /** returns the cached child els, if they exist, and the modcount of our childnodes matches the stashed modcount */
    @Nullable List<Element> cachedChildren() {
        if (attributes == null || !attributes.hasUserData()) return null; // don't create empty userdata
        Map<String, Object> userData = attributes.userData();
        //noinspection unchecked
        WeakReference<List<Element>> ref = (WeakReference<List<Element>>) userData.get(childElsKey);
        if (ref != null) {
            List<Element> els = ref.get();
            if (els != null) {
                Integer modCount = (Integer) userData.get(childElsMod);
                if (modCount != null && modCount == childNodes.modCount())
                    return els;
            }
        }
        return null;
    }

    /**
     * Maintains a shadow copy of this element's child elements. If the nodelist is changed, this cache is invalidated.
     * @return a list of child elements
     */
    List<Element> childElementsList() {
        if (childNodeSize() == 0) return EmptyChildren; // short circuit creating empty
        // set atomically, so works in multi-thread. Calling methods look like reads, so should be thread-safe
        synchronized (childNodes) { // sync vs re-entrant lock, to save another field
            List<Element> children = cachedChildren();
            if (children == null) {
                children = filterNodes(Element.class);
                stashChildren(children);
            }
            return children;
        }
    }

    @Override protected List<nokogiri.internals.html.nodes.Node> ensureChildNodes() {
        if (childNodes == EmptyNodeList) {
            childNodes = new NodeList(4);
        }
        return childNodes;
    }

    @Override
    protected void doSetBaseUri(String baseUri) {
        attributes().put(BaseUriKey, baseUri);
    }

    @Override
    protected Element doClone(@Nullable Node parent) {
        Element clone = (Element) super.doClone(parent);
        clone.childNodes = new NodeList(childNodes.size());
        clone.childNodes.addAll(childNodes); // the children then get iterated and cloned in Node.clone
        if (attributes != null) {
            clone.attributes = attributes.clone();
            // clear any cached children
            clone.attributes.userData(childElsKey, null);
        }

        return clone;
    }

    private <T> List<T> filterNodes(Class<T> clazz) {
        return childNodes.stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    /** caches the child els into the Attribute user data. */
    private void stashChildren(List<Element> els) {
        Map<String, Object> userData = attributes().userData();
        WeakReference<List<Element>> ref = new WeakReference<>(els);
        userData.put(childElsKey, ref);
        userData.put(childElsMod, childNodes.modCount());
    }

    private void ownText(StringBuilder accum) {
        for (int i = 0; i < childNodeSize(); i++) {
            Node child = childNodes.get(i);
            if (child instanceof TextNode) {
                TextNode textNode = (TextNode) child;
                appendNormalisedText(accum, textNode);
            } else if (child.nameIs("br") && !lastCharIsWhitespace(accum)) {
                accum.append(" ");
            }
        }
    }

    private static <E extends Element> int indexInList(Element search, List<E> elements) {
        final int size = elements.size();
        for (int i = 0; i < size; i++) {
            if (elements.get(i) == search)
                return i;
        }
        return 0;
    }

    private static void appendNormalisedText(StringBuilder accum, TextNode textNode) {
        String text = textNode.getWholeText();
        if (preserveWhitespace(textNode.parentNode) || textNode instanceof CDataNode)
            accum.append(text);
        else
            StringUtil.appendNormalisedWhitespace(accum, text, lastCharIsWhitespace(accum));
    }

    static boolean preserveWhitespace(@Nullable Node node) {
        // looks only at this element and five levels up, to prevent recursion & needless stack searches
        if (node instanceof Element) {
            Element el = (Element) node;
            int i = 0;
            do {
                if (el.tag.preserveWhitespace())
                    return true;
                el = el.parent();
                i++;
            } while (i < 6 && el != null);
        }
        return false;
    }

    private static String wholeTextOf(Stream<Node> stream) {
        return stream.map(node -> {
            if (node instanceof TextNode) return ((TextNode) node).getWholeText();
            if (node.nameIs("br")) return "\n";
            return "";
        }).collect(StringUtil.joining(""));
    }

    /* If XML syntax, normalizes < to _ in tag name. */
    @Nullable private String safeTagName(Document.OutputSettings.Syntax syntax) {
        return syntax == Document.OutputSettings.Syntax.xml ? Normalizer.xmlSafeTagName(tagName()) : tagName();
    }

    private static class TextAccumulator implements NodeVisitor {
        private final StringBuilder accum;

        public TextAccumulator(StringBuilder accum) {
            this.accum = accum;
        }

        @Override public void head(Node node, int depth) {
            if (node instanceof TextNode) {
                TextNode textNode = (TextNode) node;
                appendNormalisedText(accum, textNode);
            } else if (node instanceof Element) {
                Element element = (Element) node;
                if (accum.length() > 0 &&
                        (element.isBlock() || element.nameIs("br")) &&
                        !lastCharIsWhitespace(accum))
                    accum.append(' ');
            }
        }

        @Override public void tail(Node node, int depth) {
            // make sure there is a space between block tags and immediately following text nodes or inline elements <div>One</div>Two should be "One Two".
            if (node instanceof Element) {
                Element element = (Element) node;
                Node next = node.nextSibling();
                if (!element.tag.isInline() && (next instanceof TextNode || next instanceof Element && ((Element) next).tag.isInline()) && !lastCharIsWhitespace(accum))
                    accum.append(' ');
            }

        }
    }
}
