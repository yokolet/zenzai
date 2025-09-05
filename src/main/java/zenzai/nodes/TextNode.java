package zenzai.nodes;

import org.w3c.dom.DOMException;
import org.w3c.dom.Text;
import zenzai.helper.Validate;
import zenzai.internal.QuietAppendable;
import zenzai.internal.StringUtil;

public abstract class TextNode extends LeafNode implements Text {

    /**
     Create a new TextNode representing the supplied (unencoded) text).

     @param text raw text
     @see #createFromEncoded(String)
     */
    public TextNode(String text) {
        super(text);
    }

    @Override
    public String toString() {
        return outerHtml();
    }

    @Override
    public TextNode clone() {
        return (TextNode) super.clone();
    }

    // org.w3c.dom.Node
    @Override
    public String getNodeName() {
        return "#text";
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
        coreValue(value);
    }

    // org.w3c.dom.Node
    @Override
    public short getNodeType() {
        return Node.TEXT_NODE;
    }

    // org.w3c.dom.Node
    @Override
    public org.w3c.dom.NodeList getChildNodes() {
        return new Element.NodeList(0);
    }

    // jsoup.nodes.Node
    @Override public String nodeName() {
        return "#text";
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

    // org.w3c.dom.Text
    /**
     * Split this text node into two nodes at the specified string offset. After splitting, this node will contain the
     * original text up to the offset, and will have a new text node sibling containing the text after the offset.
     * @param offset string offset point to split node at.
     * @return the newly created text node containing the text after the offset.
     */
    @Override
    public Text splitText(int offset) throws DOMException {
        final String text = coreValue();
        Validate.isTrue(offset >= 0, "Split offset must be not be negative");
        Validate.isTrue(offset < text.length(), "Split offset must not be greater than current text length");

        String head = text.substring(0, offset);
        String tail = text.substring(offset);
        text(head);
        TextNode tailNode = new TextNode(tail);
        if (parentNode != null)
            parentNode.addChildren(siblingIndex()+1, tailNode);

        return tailNode;
    }

    // org.w3c.dom.Text
    @Override
    public boolean isElementContentWhitespace() {
        return isBlank();
    }

    // org.w3c.dom.Text
    /**
     Get the (unencoded) text of this text node, including any newlines and spaces present in the original.
     @return text
     */
    @Override
    public String getWholeText() {
        return coreValue();
    }

    // org.w3c.dom.Text
    @Override
    public Text replaceWholeText(String content) throws DOMException {
        coreValue(content);
        return this;
    }

    /**
     Test if this text node is blank -- that is, empty or only whitespace (including newlines).
     @return true if this document is empty or only whitespace, false if it contains any text content.
     */
    public boolean isBlank() {
        return StringUtil.isBlank(coreValue());
    }

    /**
     * Get the text content of this text node.
     * @return Unencoded, normalised text.
     * @see TextNode#getWholeText()
     */
    public String text() {
        return StringUtil.normaliseWhitespace(getWholeText());
    }

    /**
     * Set the text content of this text node.
     * @param text unencoded text
     * @return this, for chaining
     */
    public TextNode text(String text) {
        coreValue(text);
        return this;
    }

    /**
     * Create a new TextNode from HTML encoded (aka escaped) data.
     * @param encodedText Text containing encoded HTML (e.g. {@code &lt;})
     * @return TextNode containing unencoded data (e.g. {@code <})
     */
    public static TextNode createFromEncoded(String encodedText) {
        String text = Entities.unescape(encodedText);
        return new TextNode(text);
    }

    @Override
    void outerHtmlHead(QuietAppendable accum, Document.OutputSettings out) {
        Entities.escape(accum, coreValue(), out, Entities.ForText);
    }

    static String normaliseWhitespace(String text) {
        text = StringUtil.normaliseWhitespace(text);
        return text;
    }

    static String stripLeadingWhitespace(String text) {
        return text.replaceFirst("^\\s+", "");
    }

    static boolean lastCharIsWhitespace(StringBuilder sb) {
        return sb.length() != 0 && sb.charAt(sb.length() - 1) == ' ';
    }
}
