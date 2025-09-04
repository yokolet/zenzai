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

    // org.w3d.dom.Node
    @Override
    public String getNodeValue() throws DOMException {
        // TODO: check if a string length limit exists
        // DOMException.DOMSTRING_SIZE_ERR is raised when it would return more characters than fit in
        // a DOMString variable on the implementation platform. It depends on a web browser implementation.
        return getWholeText();
    }

    // org.w3c.dom.Node
    @Override
    public void setNodeValue(String value) throws DOMException {
        // NO_MODIFICATION_ALLOWED_ERR will be raised when the node is readonly and if it is not defined to be null.
        coreValue(value);
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
