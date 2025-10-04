package nokogiri.internals.html.nodes;

import java.util.List;

import org.jspecify.annotations.Nullable;

import nokogiri.internals.html.helper.Validate;
import nokogiri.internals.html.internal.QuietAppendable;

public abstract class LeafNode extends Node {
    Object value;

    public LeafNode() {
        value = "";
    }

    protected LeafNode(String coreValue) {
        Validate.notNull(coreValue);
        value = coreValue;
    }

    // org.w3c.dom.Node
    @Override
    public org.w3c.dom.Node getParentNode() {
        return parent();
    }

    // org.w3c.dom.Node
    // jsoup.nodes.Node
    @Override public final boolean hasAttributes() {
        return value instanceof Attributes;
    }

    @Override
    public final Attributes attributes() {
        ensureAttributes();
        return (Attributes) value;
    }

    @Override
    public String nodeValue() {
        return coreValue();
    }

    @Override
    public String attr(String key) {
        if (!hasAttributes()) {
            return nodeName().equals(key) ? (String) value : EmptyString;
        }
        return super.attr(key);
    }

    @Override
    public Node attr(String key, String value) {
        if (!hasAttributes() && key.equals(nodeName())) {
            this.value = value;
        } else {
            ensureAttributes();
            super.attr(key, value);
        }
        return this;
    }

    @Override
    public boolean hasAttr(String key) {
        ensureAttributes();
        return super.hasAttr(key);
    }

    @Override
    public Node removeAttr(String key) {
        ensureAttributes();
        return super.removeAttr(key);
    }

    @Override @Nullable
    public Element parent() {
        return parentNode;
    }

    @Override
    public String absUrl(String key) {
        ensureAttributes();
        return super.absUrl(key);
    }

    @Override
    public String baseUri() {
        return parentNode != null ? parentNode.baseUri() : "";
    }

    @Override
    protected void doSetBaseUri(String baseUri) {
        // noop
    }

    @Override
    public int childNodeSize() {
        return 0;
    }

    @Override
    public Node empty() {
        return this;
    }

    @Override
    protected List<Node> ensureChildNodes() {
        return EmptyNodes;
    }

    @Override
    protected LeafNode doClone(Node parent) {
        LeafNode clone = (LeafNode) super.doClone(parent);

        // Object value could be plain string or attributes - need to clone
        if (hasAttributes())
            clone.value = ((Attributes) value).clone();

        return clone;
    }

    @Override
    void outerHtmlTail(QuietAppendable accum, Document.OutputSettings out) {}

    String coreValue() {
        return attr(nodeName());
    }

    void coreValue(String value) {
        attr(nodeName(), value);
    }

    private void ensureAttributes() {
        if (!hasAttributes()) { // then value is String coreValue
            String coreValue = (String) value;
            Attributes attributes = new Attributes();
            attributes.setOwnerElement(parent());
            value = attributes;
            attributes.put(nodeName(), coreValue);
        }
    }
}
