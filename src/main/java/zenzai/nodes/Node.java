package zenzai.nodes;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.*;

import zenzai.helper.Validate;
import zenzai.helper.W3CValidation;
import zenzai.internal.QuietAppendable;
import zenzai.internal.StringUtil;
import zenzai.parser.ParseSettings;
import zenzai.parser.Parser;
import zenzai.select.NodeFilter;
import zenzai.select.NodeVisitor;

public abstract class Node implements org.w3c.dom.Node, Cloneable {
    @Nullable Element parentNode; // Nodes don't always have parents
    static final List<Node> EmptyNodes = Collections.emptyList();
    static final String EmptyString = "";
    int siblingIndex;
    org.w3c.dom.Document document;

    /**
     * Default constructor. Doesn't set up base uri, children, or attributes; use with caution.
     */
    protected Node() {
    }

    /**
     * Gets this node's outer HTML.
     * @return outer HTML.
     * @see #outerHtml()
     */
    @Override
    public String toString() {
        return outerHtml();
    }

    /**
     * Check if this node is the same instance of another (object identity test).
     * <p>For a node value equality check, see {@link #hasSameValue(Object)}</p>
     * @param o other object to compare to
     * @return true if the content of this node is the same as the other
     * @see Node#hasSameValue(Object)
     */
    @Override
    public boolean equals(@Nullable Object o) {
        // implemented just so that javadoc is clear this is an identity test
        return this == o;
    }

    /**
     Provides a hashCode for this Node, based on its object identity. Changes to the Node's content will not impact the
     result.
     @return an object identity based hashcode for this Node
     */
    @Override
    public int hashCode() {
        // implemented so that javadoc and scanners are clear this is an identity test
        return super.hashCode();
    }

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

    // org.w3c.dom.Node
    public abstract String getNodeName();
    public String getNodeValue() { return null; }
    public void setNodeValue(String nodeValue) throws DOMException {
        // no-op
    }
    public abstract short getNodeType(); // subclases implement this method
    public org.w3c.dom.Node getParentNode() { return parentNode; }
    public org.w3c.dom.NodeList getChildNodes() { return new zenzai.nodes.Element.NodeList(0); }
    public org.w3c.dom.Node getFirstChild() { return null; }
    public org.w3c.dom.Node getLastChild() { return null; }
    public org.w3c.dom.Node getPreviousSibling() { return nextSibling(); }
    public org.w3c.dom.Node getNextSibling() { return previousSibling(); }
    public NamedNodeMap getAttributes() { return null; }
    public Document getOwnerDocument() { return ownerDocument(); }
    public org.w3c.dom.Node insertBefore(org.w3c.dom.Node newChild, org.w3c.dom.Node refChild) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported for this type of node.");
    }
    public org.w3c.dom.Node replaceChild(org.w3c.dom.Node newChild, org.w3c.dom.Node oldChild) throws DOMException {
        W3CValidation.hierarchyRequest(this, (zenzai.nodes.Node)newChild);
        W3CValidation.wrongDocument(this, (zenzai.nodes.Node)newChild);
        W3CValidation.modificationAllowed(this, (zenzai.nodes.Node)newChild);
        W3CValidation.nodeInChildren(this, (zenzai.nodes.Node)oldChild);
        W3CValidation.operationSupported(this);
        replaceChild(oldChild, newChild);
        return oldChild;
    }
    public org.w3c.dom.Node removeChild(org.w3c.dom.Node oldChild) throws DOMException {
        W3CValidation.modificationAllowed(this);
        W3CValidation.nodeInChildren(this, (zenzai.nodes.Node)oldChild);
        W3CValidation.operationSupported(this);
        removeChild(oldChild);
        return oldChild;
    }
    public org.w3c.dom.Node appendChild(org.w3c.dom.Node newChild) throws DOMException {
        W3CValidation.hierarchyRequest(this, (zenzai.nodes.Node)newChild);
        W3CValidation.wrongDocument(this, (zenzai.nodes.Node)newChild);
        W3CValidation.modificationAllowed(this, (zenzai.nodes.Node)newChild);
        W3CValidation.operationSupported(((zenzai.nodes.Node) newChild).parentNode());
        addChildren((zenzai.nodes.Node)newChild);
        return newChild;
    }
    public boolean hasChildNodes() { return childNodeSize() > 0; }
    public org.w3c.dom.Node cloneNode(boolean deep) {
        Node cloned;
        if (deep) { cloned = clone(); }
        else { cloned = shallowClone(); }
        return cloned;
    }
    public void normalize() {
        // TODO: implement this method
        // see: https://developer.mozilla.org/en-US/docs/Web/API/Node/normalize
        // no-op for now
    }
    public boolean isSupported(String feature, String version) { return false;}
    public String getNamespaceURI() { return Parser.NamespaceHtml; }
    public String getPrefix() { return null; }
    public void setPrefix(String prefix) throws DOMException {
        // no-op
    }
    public String getLocalName() {
        // TODO: check return values from ELEMENT_NODE and ATTRIBUTE_NODE
        return null;
    }
    public boolean hasAttributes() { return false; }
    public String getBaseURI() { return null; }
    public short compareDocumentPosition(org.w3c.dom.Node other) throws DOMException {
        // TODO: decide if the implementation of this method is required
        // see: https://developer.mozilla.org/en-US/docs/Web/API/Node/compareDocumentPosition
        return 0;
    }
    public abstract String getTextContent() throws DOMException; // implementation leaves to subclasses
    public abstract void setTextContent(String textContent) throws DOMException; // implementation leaves to subcalsses
    public boolean isSameNode(org.w3c.dom.Node other) {
        return this.hashCode() == other.hashCode();
    }
    public String lookupPrefix(String namespaceURI) { return null; }
    public boolean isDefaultNamespace(String namespaceURI) { return namespaceURI.equals(Parser.NamespaceHtml); }
    public String lookupNamespaceURI(String prefix) { return null; }
    public boolean isEqualNode(org.w3c.dom.Node arg) {
        // TODO: check correctness of this method
        if (getNodeType() != arg.getNodeType()) return false;
        if (!getNodeName().equals(arg.getNodeName())) return false;
        if (!getLocalName().equals(arg.getLocalName())) return false;
        if (!getNodeValue().equals(arg.getNodeValue())) return false;
        if (!getAttributes().equals(arg.getAttributes())) return false;
        if (!childNodes().equals(arg.getChildNodes())) return false;
        return true;
    }
    public Object getFeature(String feature, String version) { return null; }
    public Object setUserData(String key, Object data, UserDataHandler handler) {
        // TODO: guess HTML5 doesn't have user data feature
        return null;
    }
    public Object getUserData(String key) {
        // TODO: same as above. HTML5 doesn't have user data feature
        return null;
    }

    /**
     * Get each of the Element's attributes.
     * @return attributes (which implements Iterable, with the same order as presented in the original HTML).
     */
    public abstract Attributes attributes();

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
     Gets the first sibling of this node. That may be this node.

     @return the first sibling node
     @since 1.21.1
     */
    public Node firstSibling() {
        if (parentNode != null) {
            //noinspection DataFlowIssue
            return parentNode.firstChild();
        } else
            return this; // orphan is its own first sibling
    }

    /**
     Gets the last sibling of this node. That may be this node.

     @return the last sibling (aka the parent's last child)
     @since 1.21.1
     */
    public Node lastSibling() {
        if (parentNode != null) {
            //noinspection DataFlowIssue (not nullable, would be this if no other sibs)
            return parentNode.lastChild();
        } else
            return this;
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
     Get the number of attributes that this Node has.
     @return the number of attributes
     @since 1.14.2
     */
    public int attributesSize() {
        // added so that we can test how many attributes exist without implicitly creating the Attributes object
        return hasAttributes() ? attributes().size() : 0;
    }

    /**
     * Gets the Document associated with this Node.
     * @return the Document associated with this Node, or null if there is no such Document.
     */
    public @Nullable Document ownerDocument() {
        if (document != null) { return (zenzai.nodes.Document)document; }
        zenzai.nodes.Node node = this;
        while (node != null) {
            if (node instanceof Document) {
                document = (Document)node;
                return (Document) node;
            }
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
     * Insert the specified HTML into the DOM before this node (as a preceding sibling).
     * @param html HTML to add before this node
     * @return this node, for chaining
     * @see #after(String)
     */
    public Node before(String html) {
        addSiblingHtml(siblingIndex(), html);
        return this;
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
     * Insert the specified HTML into the DOM after this node (as a following sibling).
     * @param html HTML to add after this node
     * @return this node, for chaining
     * @see #before(String)
     */
    public Node after(String html) {
        addSiblingHtml(siblingIndex() + 1, html);
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
     Wrap the supplied HTML around this node.

     @param html HTML to wrap around this node, e.g. {@code <div class="head"></div>}. Can be arbitrarily deep. If
     the input HTML does not parse to a result starting with an Element, this will be a no-op.
     @return this node, for chaining.
     */
    public Node wrap(String html) {
        Validate.notEmpty(html);

        // Parse context - parent (because wrapping), this, or null
        Element context =
                parentNode != null && parentNode instanceof Element ? (Element) parentNode :
                        this instanceof Element ? (Element) this :
                                null;
        List<Node> wrapChildren = NodeUtils.parser(this).parseFragmentInput(html, context, baseUri());
        Node wrapNode = wrapChildren.get(0);
        if (!(wrapNode instanceof Element)) // nothing to wrap with; noop
            return this;

        Element wrap = (Element) wrapNode;
        Element deepest = getDeepChild(wrap);
        if (parentNode != null)
            parentNode.replaceChild(this, wrap);
        deepest.addChildren(this); // side effect of tricking wrapChildren to lose first

        // remainder (unbalanced wrap, like <div></div><p></p> -- The <p> is remainder
        if (wrapChildren.size() > 0) {
            //noinspection ForLoopReplaceableByForEach (beacause it allocates an Iterator which is wasteful here)
            for (int i = 0; i < wrapChildren.size(); i++) {
                Node remainder = wrapChildren.get(i);
                // if no parent, this could be the wrap node, so skip
                if (wrap == remainder)
                    continue;

                if (remainder.parentNode != null)
                    remainder.parentNode.removeChild(remainder);
                wrap.after(remainder);
            }
        }
        return this;
    }

    /**
     * Removes this node from the DOM, and moves its children up into the node's parent. This has the effect of dropping
     * the node but keeping its children.
     * <p>
     * For example, with the input html:
     * </p>
     * <p>{@code <div>One <span>Two <b>Three</b></span></div>}</p>
     * Calling {@code element.unwrap()} on the {@code span} element will result in the html:
     * <p>{@code <div>One Two <b>Three</b></div>}</p>
     * and the {@code "Two "} {@link TextNode} being returned.
     *
     * @return the first child of this node, after the node has been unwrapped. @{code Null} if the node had no children.
     * @see #remove()
     * @see #wrap(String)
     */
    public @Nullable Node unwrap() {
        Validate.notNull(parentNode);
        Node firstChild = firstChild();
        parentNode.addChildren(siblingIndex(), this.childNodesAsArray());
        this.remove();

        return firstChild;
    }

    /**
     * Replace this node in the DOM with the supplied node.
     * @param in the node that will replace the existing node.
     */
    public void replaceWith(Node in) {
        Validate.notNull(in);
        if (parentNode == null) parentNode = in.parentNode; // allows old to have been temp removed before replacing
        Validate.notNull(parentNode);
        parentNode.replaceChild(this, in);
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
     * Returns a deep copy of this node's children. Changes made to these nodes will not be reflected in the original
     * nodes
     * @return a deep copy of this node's children
     */
    public List<Node> childNodesCopy() {
        final List<Node> nodes = ensureChildNodes();
        final ArrayList<Node> children = new ArrayList<>(nodes.size());
        for (Node node : nodes) {
            children.add(node.clone());
        }
        return children;
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
     Test if this node's parent is an Element with the specified normalized name and namespace.
     * @param normalName a normalized element name (e.g. {@code div}).
     * @param namespace the namespace
     * @return true if the parent element's normal name matches exactly, and that element is in the specified namespace
     * @since 1.17.2
     */
    public boolean parentElementIs(String normalName, String namespace) {
        return parentNode != null && parentNode instanceof Element
                && ((Element) parentNode).elementIs(normalName, namespace);
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
     Returns a Stream of this Node and all of its descendant Nodes. The stream has document order.
     @return a stream of all nodes.
     @see Element#stream()
     @since 1.17.1
     */
    public Stream<Node> nodeStream() {
        return NodeUtils.stream(this, Node.class);
    }

    /**
     Returns a Stream of this and descendant nodes, containing only nodes of the specified type. The stream has document
     order.
     @return a stream of nodes filtered by type.
     @see Element#stream()
     @since 1.17.1
     */
    public <T extends Node> Stream<T> nodeStream(Class<T> type) {
        return NodeUtils.stream(this, type);
    }

    /**
     * Clear (remove) each of the attributes in this node.
     * @return this, for chaining
     */
    public Node clearAttributes() {
        if (hasAttributes()) {
            Iterator<Attribute> it = attributes().iterator();
            while (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
        return this;
    }

    /**
     * Get this node's root node; that is, its topmost ancestor. If this node is the top ancestor, returns {@code this}.
     * @return topmost ancestor.
     */
    public Node root() {
        Node node = this;
        while (node.parentNode != null)
            node = node.parentNode;
        return node;
    }

    /**
     Perform the supplied action on this Node and each of its descendants, during a depth-first traversal. Nodes may be
     inspected, changed, added, replaced, or removed.
     @param action the function to perform on the node
     @return this Node, for chaining
     @see Element#forEach(Consumer)
     */
    public Node forEachNode(Consumer<? super Node> action) {
        Validate.notNull(action);
        nodeStream().forEach(action);
        return this;
    }

    /**
     * Perform a depth-first controllable traversal through this node and its descendants.
     * @param nodeFilter the filter callbacks to perform on each node
     * @return this node, for chaining
     */
    public Node filter(NodeFilter nodeFilter) {
        Validate.notNull(nodeFilter);
        nodeFilter.traverse(this);
        return this;
    }

    /**
     * Check if this node has the same content as another node. A node is considered the same if its name, attributes and content match the
     * other node; particularly its position in the tree does not influence its similarity.
     * @param o other object to compare to
     * @return true if the content of this node is the same as the other
     */
    public boolean hasSameValue(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return this.outerHtml().equals(((Node) o).outerHtml());
    }

    protected void setOwnerDocument(Document ownerDocument) {
        document = ownerDocument;
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

    protected void addChildren(Node... children) {
        //most used. short circuit addChildren(int), which hits reindex children and array copy
        final List<Node> nodes = ensureChildNodes();

        for (Node child: children) {
            reparentChild(child);
            nodes.add(child);
            child.setSiblingIndex(nodes.size()-1);
        }
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


    protected void removeChild(Node out) {
        Validate.isTrue(out.parentNode == this);
        Element el = (Element) this;
        if (el.hasValidChildren()) // can remove by index
            ensureChildNodes().remove(out.siblingIndex);
        else
            ensureChildNodes().remove(out); // iterates, but potentially not every one

        el.invalidateChildren();
        out.parentNode = null;
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

    protected Node[] childNodesAsArray() {
        return ensureChildNodes().toArray(new Node[0]);
    }

    /**
     Get the outer HTML of this node.

     @param accum accumulator to place HTML into
     @param out
     */
    abstract void outerHtmlHead(final QuietAppendable accum, final Document.OutputSettings out);

    abstract void outerHtmlTail(final QuietAppendable accum, final Document.OutputSettings out);

    private static Element getDeepChild(Element el) {
        Element child = el.firstElementChild();
        while (child != null) {
            el = child;
            child = child.firstElementChild();
        }
        return el;
    }

    private void addSiblingHtml(int index, String html) {
        Validate.notNull(html);
        Validate.notNull(parentNode);

        Element context = parentNode instanceof Element ? (Element) parentNode : null;
        List<Node> nodes = NodeUtils.parser(this).parseFragmentInput(html, context, baseUri());
        parentNode.addChildren(index, nodes.toArray(new Node[0]));
    }
}
