package zenzai.nodes;

import java.util.*;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.*;

import zenzai.helper.Validate;
import zenzai.internal.QuietAppendable;
import zenzai.internal.StringUtil;
import zenzai.parser.ParseSettings;
import zenzai.select.NodeVisitor;

public abstract class Node implements org.w3c.dom.Node, Cloneable {
    @Nullable Element parentNode; // Nodes don't always have parents
    static final List<Node> EmptyNodes = Collections.emptyList();
    static final String EmptyString = "";
    int siblingIndex;

    public abstract String getNodeName();
    public abstract String getNodeValue();
    public abstract void setNodeValue(String nodeValue) throws DOMException;
    public abstract short getNodeType();
    public abstract org.w3c.dom.Node getParentNode();
    public abstract NodeList getChildNodes();
    public abstract org.w3c.dom.Node getFirstChild();
    public abstract org.w3c.dom.Node getLastChild();
    public abstract org.w3c.dom.Node getPreviousSibling();
    public abstract org.w3c.dom.Node getNextSibling();
    public abstract NamedNodeMap getAttributes();
    public abstract Document getOwnerDocument();
    public abstract org.w3c.dom.Node insertBefore(org.w3c.dom.Node newChild, org.w3c.dom.Node refChild) throws DOMException;
    public abstract org.w3c.dom.Node replaceChild(org.w3c.dom.Node newChild, org.w3c.dom.Node oldChild) throws DOMException;
    public abstract org.w3c.dom.Node removeChild(org.w3c.dom.Node oldChild) throws DOMException;
    public abstract org.w3c.dom.Node appendChild(org.w3c.dom.Node newChild) throws DOMException;
    public abstract boolean hasChildNodes();
    public abstract org.w3c.dom.Node cloneNode(boolean deep);
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
     Get the node's value. For a TextNode, the whole text; for a Comment, the comment data; for an Element,
     wholeOwnText. Returns "" if there is no value.
     @return the node's value
     */
    public String nodeValue() {
        return "";
    }

    /**
     Get the base URI that applies to this node. Will return an empty string if not defined. Used to make relative links
     absolute.

     @return base URI
     @see #absUrl
     */
    public abstract String baseUri();

    /**
     * Delete all this node's children.
     * @return this node, for chaining
     */
    public abstract Node empty();

    /**
     * Get each of the Element's attributes.
     * @return attributes (which implements Iterable, with the same order as presented in the original HTML).
     */
    public abstract Attributes attributes();

    @Override
    public zenzai.nodes.Node clone() {
        zenzai.nodes.Node thisClone = doClone(null); // splits for orphan

        // Queue up nodes that need their children cloned (BFS).
        final LinkedList<zenzai.nodes.Node> nodesToProcess = new LinkedList<>();
        nodesToProcess.add(thisClone);

        while (!nodesToProcess.isEmpty()) {
            zenzai.nodes.Node currParent = nodesToProcess.remove();

            final int size = currParent.childNodeSize();
            for (int i = 0; i < size; i++) {
                final List<zenzai.nodes.Node> childNodes = currParent.ensureChildNodes();
                zenzai.nodes.Node childClone = childNodes.get(i).doClone(currParent);
                childNodes.set(i, childClone);
                nodesToProcess.add(childClone);
            }
        }
        return thisClone;
    }

    /**
     * Create a stand-alone, shallow copy of this node. None of its children (if any) will be cloned, and it will have
     * no parent or sibling nodes.
     * @return a single independent copy of this node
     * @see #clone()
     */
    public zenzai.nodes.Node shallowClone() {
        return doClone(null);
    }

    /**
     Gets the first child node of this node, or {@code null} if there is none. This could be any Node type, such as an
     Element, TextNode, Comment, etc. Use {@link Element#firstElementChild()} to get the first Element child.
     @return the first child node, or null if there are no children.
     @see Element#firstElementChild()
     @see #lastChild()
     @since 1.15.2
     */
    public @Nullable Node firstChild() {
        if (childNodeSize() == 0) return null;
        return ensureChildNodes().get(0);
    }

    /**
     Gets the last child node of this node, or {@code null} if there is none.
     @return the last child node, or null if there are no children.
     @see Element#lastElementChild()
     @see #firstChild()
     @since 1.15.2
     */
    public @Nullable Node lastChild() {
        final int size = childNodeSize();
        if (size == 0) return null;
        List<Node> children = ensureChildNodes();
        return children.get(size - 1);
    }

    /**
     * Perform a depth-first traversal through this node and its descendants.
     * @param nodeVisitor the visitor callbacks to perform on each node
     * @return this node, for chaining
     */
    public zenzai.nodes.Node traverse(NodeVisitor nodeVisitor) {
        Validate.notNull(nodeVisitor);
        nodeVisitor.traverse(this);
        return this;
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
        zenzai.nodes.Node node = this;
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
    public @Nullable final Node parentNode() {
        return parentNode;
    }

    /**
     Gets this node's parent node. This is always an Element.
     @return parent node; or null if no parent.
     @see #hasParent()
     @see #parentElement();
     */
    public @Nullable Node parent() {
        return parentNode;
    }

    /**
     Gets this node's parent Element.
     @return parent element; or null if this node has no parent.
     @see #hasParent()
     @since 1.21.1
     */
    public @Nullable Element parentElement() {
        return parentNode;
    }

    /**
     * Insert the specified node into the DOM before this node (as a preceding sibling).
     * @param node to add before this node
     * @return this node, for chaining
     * @see #after(Node)
     */
    public Node before(zenzai.nodes.Node node) {
        Validate.notNull(node);
        Validate.notNull(parentNode);

        // if the incoming node is a sibling of this, remove it first so siblingIndex is correct on add
        if (node.parentNode == parentNode) node.remove();

        parentNode.addChildren(siblingIndex(), node);
        return this;
    }

    /**
     * Insert the specified node into the DOM after this node (as a following sibling).
     * @param node to add after this node
     * @return this node, for chaining
     * @see #before(Node)
     */
    public Node after(zenzai.nodes.Node node) {
        Validate.notNull(node);
        Validate.notNull(parentNode);

        // if the incoming node is a sibling of this, remove it first so siblingIndex is correct on add
        if (node.parentNode == parentNode) node.remove();

        parentNode.addChildren(siblingIndex() + 1, node);
        return this;
    }

    /**
     * Remove (delete) this node from the DOM tree. If this node has children, they are also removed. If this node is
     * an orphan, nothing happens.
     */
    public void remove() {
        if (parentNode != null)
            parentNode.removeChild(this);
    }

    /**
     Get this node's children. Presented as an unmodifiable list: new children can not be added, but the child nodes
     themselves can be manipulated.
     @return list of children. If no children, returns an empty list.
     */
    public List<zenzai.nodes.Node> childNodes() {
        if (childNodeSize() == 0)
            return EmptyNodes;

        List<zenzai.nodes.Node> children = ensureChildNodes();
        List<zenzai.nodes.Node> rewrap = new ArrayList<>(children.size()); // wrapped so that looping and moving will not throw a CME as the source changes
        rewrap.addAll(children);
        return Collections.unmodifiableList(rewrap);
    }

    /**
     Retrieves this node's sibling nodes. Similar to {@link #childNodes() node.parent.childNodes()}, but does not
     include this node (a node is not a sibling of itself).
     @return node siblings. If the node has no parent, returns an empty list.
     */
    public List<zenzai.nodes.Node> siblingNodes() {
        if (parentNode == null)
            return Collections.emptyList();

        List<zenzai.nodes.Node> nodes = parentNode.ensureChildNodes();
        List<zenzai.nodes.Node> siblings = new ArrayList<>(nodes.size() - 1);
        for (zenzai.nodes.Node node: nodes)
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

    /**
     * Remove an attribute from this node.
     * @param attributeKey The attribute to remove.
     * @return this (for chaining)
     */
    public Node removeAttr(String attributeKey) {
        Validate.notNull(attributeKey);
        if (hasAttributes())
            attributes().removeIgnoreCase(attributeKey);
        return this;
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
     * Set an attribute (key=value). If the attribute already exists, it is replaced. The attribute key comparison is
     * <b>case insensitive</b>. The key will be set with case sensitivity as set in the parser settings.
     * @param attributeKey The attribute key.
     * @param attributeValue The attribute value.
     * @return this (for chaining)
     */
    public Node attr(String attributeKey, String attributeValue) {
        Document doc = ownerDocument();
        ParseSettings settings = doc != null ? doc.parser().settings() : ParseSettings.htmlDefault;
        attributeKey = settings.normalizeAttribute(attributeKey);
        attributes().putIgnoreCase(attributeKey, attributeValue);
        return this;
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

    /**
     Gets the next sibling Element of this node. E.g., if a {@code div} contains two {@code p}s, the
     {@code nextElementSibling} of the first {@code p} is the second {@code p}.
     <p>This is similar to {@link #nextSibling()}, but specifically finds only Elements.</p>

     @return the next element, or null if there is no next element
     @see #previousElementSibling()
     */
    public @Nullable Element nextElementSibling() {
        zenzai.nodes.Node next = this;
        while ((next = next.nextSibling()) != null) {
            if (next instanceof Element) return (Element) next;
        }
        return null;
    }

    /**
     Gets the previous Element sibling of this node.

     @return the previous element, or null if there is no previous element
     @see #nextElementSibling()
     */
    public @Nullable Element previousElementSibling() {
        Node prev = this;
        while ((prev = prev.previousSibling()) != null) {
            if (prev instanceof Element) return (Element) prev;
        }
        return null;
    }

    /**
     Get this node's next sibling.
     @return next sibling, or {@code null} if this is the last sibling
     */
    public @Nullable Node nextSibling() {
        if (parentNode == null)
            return null; // root

        final List<zenzai.nodes.Node> siblings = parentNode.ensureChildNodes();
        final int index = siblingIndex() + 1;
        if (siblings.size() > index) {
            zenzai.nodes.Node node = siblings.get(index);
            assert (node.siblingIndex == index); // sanity test that invalidations haven't missed
            return node;
        } else
            return null;
    }

    /**
     Get this node's previous sibling.
     @return the previous sibling, or @{code null} if this is the first sibling
     */
    public @Nullable Node previousSibling() {
        if (parentNode == null)
            return null; // root

        if (siblingIndex() > 0)
            return parentNode.ensureChildNodes().get(siblingIndex-1);
        else
            return null;
    }

    /**
     Checks if this node has a parent. Nodes won't have parents if (e.g.) they are newly created and not added as a child
     to an existing node, or if they are a {@link #shallowClone()}. In such cases, {@link #parent()} will return {@code null}.
     @return if this node has a parent.
     */
    public boolean hasParent() {
        return parentNode != null;
    }

    /**
     Test if this node's parent has the specified normalized name.
     * @param normalName a normalized name (e.g. {@code div}).
     * @return true if the parent element's normal name matches exactly
     * @since 1.17.2
     */
    public boolean parentNameIs(String normalName) {
        return parentNode != null && parentNode.normalName().equals(normalName);
    }

    /**
     Get a child node by its 0-based index.
     @param index index of child node
     @return the child node at this index.
     @throws IndexOutOfBoundsException if the index is out of bounds.
     */
    public zenzai.nodes.Node childNode(int index) {
        return ensureChildNodes().get(index);
    }

    /**
     Get the source range (start and end positions) in the original input source from which this node was parsed.
     Position tracking must be enabled prior to parsing the content. For an Element, this will be the positions of the
     start tag.
     @return the range for the start of the node, or {@code untracked} if its range was not tracked.
     @see org.jsoup.parser.Parser#setTrackPosition(boolean)
     @see Range#isImplicit()
     @see Element#endSourceRange()
     @see Attributes#sourceRange(String name)
     @since 1.15.2
     */
    public Range sourceRange() {
        return Range.of(this, true);
    }

    /**
     Get the outer HTML of this node. For example, on a {@code p} element, may return {@code <p>Para</p>}.
     @return outer HTML
     @see Element#html()
     @see Element#text()
     */
    public String outerHtml() {
        StringBuilder sb = StringUtil.borrowBuilder();
        outerHtml(QuietAppendable.wrap(sb));
        return StringUtil.releaseBuilder(sb);
    }

    /**
     Write this node and its children to the given {@link Appendable}.

     @param appendable the {@link Appendable} to write to.
     @return the supplied {@link Appendable}, for chaining.
     @throws org.jsoup.SerializationException if the appendable throws an IOException.
     */
    public <T extends Appendable> T html(T appendable) {
        outerHtml(appendable);
        return appendable;
    }

    /**
     * Set the baseUri for just this node (not its descendants), if this Node tracks base URIs.
     * @param baseUri new URI
     */
    protected abstract void doSetBaseUri(String baseUri);

    protected abstract List<zenzai.nodes.Node> ensureChildNodes();

    protected zenzai.nodes.Node doClone(@Nullable Node parent) {
        assert parent == null || parent instanceof Element;
        zenzai.nodes.Node clone;

        try {
            clone = (zenzai.nodes.Node) super.clone();
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

    protected void addChildren(int index, zenzai.nodes.Node... children) {
        // todo clean up all these and use the list, not the var array. just need to be careful when iterating the incoming (as we are removing as we go)
        Validate.notNull(children);
        if (children.length == 0) return;
        final List<zenzai.nodes.Node> nodes = ensureChildNodes();

        // fast path - if used as a wrap (index=0, children = child[0].parent.children - do inplace
        final zenzai.nodes.Node firstParent = children[0].parent();
        if (firstParent != null && firstParent.childNodeSize() == children.length) {
            boolean sameList = true;
            final List<zenzai.nodes.Node> firstParentNodes = firstParent.ensureChildNodes();
            // identity check contents to see if same
            int i = children.length;
            while (i-- > 0) {
                if (children[i] != firstParentNodes.get(i)) {
                    sameList = false;
                    break;
                }
            }
            if (sameList) { // moving, so OK to empty firstParent and short-circuit
                firstParent.empty();
                nodes.addAll(index, Arrays.asList(children));
                i = children.length;
                assert this instanceof Element;
                while (i-- > 0) {
                    children[i].parentNode = (Element) this;
                }
                ((Element) this).invalidateChildren();
                return;
            }
        }

        Validate.noNullElements(children);
        for (zenzai.nodes.Node child : children) {
            reparentChild(child);
        }
        nodes.addAll(index, Arrays.asList(children));
        ((Element) this).invalidateChildren();
    }

    protected void reparentChild(zenzai.nodes.Node child) {
        child.setParentNode(this);
    }

    protected void setParentNode(zenzai.nodes.Node parentNode) {
        Validate.notNull(parentNode);
        if (this.parentNode != null)
            this.parentNode.removeChild(this);
        assert parentNode instanceof Element;
        this.parentNode = (Element) parentNode;
    }

    protected void setSiblingIndex(int siblingIndex) {
        this.siblingIndex = siblingIndex;
    }

    protected void outerHtml(Appendable accum) {
        outerHtml(QuietAppendable.wrap(accum));
    }

    protected void outerHtml(QuietAppendable accum) {
        Printer printer = Printer.printerFor(this, accum);
        printer.traverse(this);
    }

    /**
     Get the outer HTML of this node.

     @param accum accumulator to place HTML into
     @param out
     */
    abstract void outerHtmlHead(final QuietAppendable accum, final Document.OutputSettings out);

    abstract void outerHtmlTail(final QuietAppendable accum, final Document.OutputSettings out);
}
