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

    // org.w3c.dom.Node
    @Override public String nodeName() {
        return "#comment";
    }

    // org.w3c.dom.CharacterData
    public abstract String getData() throws DOMException;
    public abstract void setData(String data) throws DOMException;
    public abstract int getLength();
    public abstract String substringData(int offset, int length) throws DOMException;
    public abstract void appendData(String data) throws DOMException;
    public abstract void insertData(int offset, String data) throws DOMException;
    public abstract void deleteData(int offset, int count) throws DOMException;
    public abstract void replaceData(int offset, int count, String arg) throws DOMException;
}
