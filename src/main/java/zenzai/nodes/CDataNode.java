package zenzai.nodes;

import org.w3c.dom.CDATASection;
import org.w3c.dom.DOMException;

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
        return getWholeText();
    }

    @Override
    public String nodeName() {
        return "#cdata";
    }
}
