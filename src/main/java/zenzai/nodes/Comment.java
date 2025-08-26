package zenzai.nodes;

import org.w3c.dom.DOMException;

public abstract class Comment extends LeafNode implements org.w3c.dom.Comment {

    /**
     Create a new comment node.
     @param data The contents of the comment
     */
    public Comment(String data) {
        super(data);
    }

    // org.w3c.dom.Node
    @Override
    public String getNodeName() {
        return "#comment";
    }

    // org.w3d.dom.Node
    @Override
    public String getNodeValue() throws DOMException {
        return getNodeValue();
    }

    @Override public String nodeName() {
        return "#comment";
    }
}
