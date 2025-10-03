package nokogiri.internals.html.nodes;

import org.w3c.dom.DOMException;

import nokogiri.internals.html.helper.W3CValidation;
import nokogiri.internals.html.internal.QuietAppendable;

public class Comment extends LeafNode implements org.w3c.dom.Comment {

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

    // org.w3c.dom.Node
    @Override
    public void setTextContent(String textContent) throws DOMException {
        coreValue(textContent);
    }

    // org.w3c.dom.CharacterData
    // nodes.Comment
    @Override
    public String getData() throws DOMException { return coreValue(); }

    // org.w3c.dom.CharacterData
    @Override
    public void setData(String data) throws DOMException { coreValue(data); }

    // org.w3c.dom.CharacterData
    @Override
    public int getLength() { return coreValue().length(); }

    // org.w3c.dom.CharacterData
    @Override
    public String substringData(int offset, int count) throws DOMException {
        W3CValidation.indexSizeWithinLength(this, offset, count);
        return coreValue().substring(offset, offset + count);
    }

    // org.w3c.dom.CharacterData
    @Override
    public void appendData(String data) throws DOMException {
        W3CValidation.modificationAllowed(this);
        coreValue(String.join(coreValue(), data));
    }

    // org.w3c.dom.CharacterData
    @Override
    public void insertData(int offset, String data) throws DOMException {
        W3CValidation.indexSizeWithinLength(this, offset);
        W3CValidation.modificationAllowed(this);
        coreValue(String.join(coreValue().substring(0, offset), data, coreValue().substring(offset)));
    }

    // org.w3c.dom.CharacterData
    @Override
    public void deleteData(int offset, int count) throws DOMException {
        W3CValidation.indexSizeWithinLength(this, offset, count);
        W3CValidation.modificationAllowed(this);
        coreValue(String.join(coreValue().substring(0, offset), coreValue().substring(offset + count)));
    }

    // org.w3c.dom.CharacterData
    @Override
    public void replaceData(int offset, int count, String arg) throws DOMException {
        W3CValidation.indexSizeWithinLength(this, offset, count);
        W3CValidation.modificationAllowed(this);
        coreValue(String.join(coreValue().substring(0, offset), arg.substring(0, count), coreValue().substring(offset + count)));
    }

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
