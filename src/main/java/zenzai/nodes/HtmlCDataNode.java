package zenzai.nodes;

import org.w3c.dom.CDATASection;

public abstract class HtmlCDataNode extends HtmlTextNode implements CDATASection {
    public HtmlCDataNode(String text) {
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
