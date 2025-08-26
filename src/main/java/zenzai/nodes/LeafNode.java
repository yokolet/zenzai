package zenzai.nodes;

import org.jspecify.annotations.Nullable;
import zenzai.helper.Validate;

public abstract class LeafNode extends zenzai.nodes.Node {
    Object value;

    public LeafNode() {
        value = "";
    }

    @Override
    public final Attributes attributes() {
        ensureAttributes();
        return (Attributes) value;
    }

    @Override @Nullable
    public Element parent() {
        return parentNode;
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

    @Override
    protected LeafNode doClone(Node parent) {
        LeafNode clone = (LeafNode) super.doClone(parent);

        // Object value could be plain string or attributes - need to clone
        if (hasAttributes())
            clone.value = ((Attributes) value).clone();

        return clone;
    }

    protected LeafNode(String coreValue) {
        Validate.notNull(coreValue);
        value = coreValue;
    }

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
