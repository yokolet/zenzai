package zenzai.nodes;

import org.w3c.dom.CDATASection;
import org.w3c.dom.DOMException;
import zenzai.internal.QuietAppendable;

public abstract class CDataNode extends TextNode implements CDATASection {
    public CDataNode(String text) {
        super(text);
    }

    // org.w3c.dom.Node
    @Override
    public String getNodeName() {
        return "#cdata-section";
    }

    @Override
    public String nodeName() {
        return "#cdata";
    }

    /**
     * Get the un-encoded, <b>non-normalized</b> text content of this CDataNode.
     * @return un-encoded, non-normalized text
     */
    @Override
    public String text() {
        return getWholeText();
    }

    @Override
    public CDataNode clone() {
        return (CDataNode) super.clone();
    }

    @Override
    void outerHtmlHead(QuietAppendable accum, Document.OutputSettings out) {
        accum
                .append("<![CDATA[")
                .append(getWholeText())
                .append("]]>");
    }
}
