package zenzai.nodes;

import org.w3c.dom.DOMException;
import zenzai.internal.QuietAppendable;

public abstract class Comment extends LeafNode implements org.w3c.dom.Comment {

    /**
     * Create a new comment node.
     *
     * @param data The contents of the comment
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
        return coreValue();
    }

    // org.w3c.dom.Node
    @Override
    public void setNodeValue(String value) throws DOMException {
        // NO_MODIFICATION_ALLOWED_ERR will be raised when the node is readonly and if it is not defined to be null.
        coreValue(value);
    }

    // org.w3c.dom.Node
    @Override
    public short getNodeType() {
        return Node.COMMENT_NODE;
    }

    // org.w3c.dom.Node
    @Override
    public String getTextContent() throws DOMException {
        return coreValue();
    }

    // org.w3c.dom.CharacterData
    // nodes.Comment
    @Override
    public String getData() throws DOMException {
        return coreValue();
    }

    public abstract void setData(String data) throws DOMException;

    public abstract int getLength();

    public abstract String substringData(int offset, int length) throws DOMException;

    public abstract void appendData(String data) throws DOMException;

    public abstract void insertData(int offset, String data) throws DOMException;

    public abstract void deleteData(int offset, int count) throws DOMException;

    public abstract void replaceData(int offset, int count, String arg) throws DOMException;

    @Override
    public Comment clone() {
        return (Comment) super.clone();
    }

    @Override
    public String nodeName() {
        return "#comment";
    }

    public Comment setDataWithReturn(String data) {
        coreValue(data);
        return this;
    }

    @Override
    void outerHtmlHead(QuietAppendable accum, Document.OutputSettings out) {
        accum
                .append("<!--")
                .append(getData())
                .append("-->");
    }

    private static boolean isXmlDeclarationData(String data) {
        return (data.length() > 1 && (data.startsWith("!") || data.startsWith("?")));
    }
}
