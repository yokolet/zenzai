package zenzai.nodes;

import zenzai.helper.Validate;

public abstract class LeafNode extends HtmlNode {
    Object value;

    public LeafNode() {
        value = "";
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
}
