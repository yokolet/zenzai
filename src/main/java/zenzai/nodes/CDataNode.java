package zenzai.nodes;

import org.w3c.dom.CDATASection;

public abstract class CDataNode extends TextNode implements CDATASection {
    public CDataNode(String text) {
        super(text);
    }

    @Override
    public String getNodeName() {
        return "#cdata-section";
    }

    @Override
    public String nodeName() {
        return "#cdata";
    }
}
