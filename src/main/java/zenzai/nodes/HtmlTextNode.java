package zenzai.nodes;

import org.w3c.dom.DOMException;
import org.w3c.dom.Text;
import zenzai.helper.Validate;
import zenzai.internal.StringUtil;

public class HtmlTextNode extends LeafNode implements Text {

    /**
     Create a new TextNode representing the supplied (unencoded) text).

     @param text raw text
     @see #createFromEncoded(String)
     */
    public HtmlTextNode(String text) {
        super(text);
    }

    @Override
    public String getNodeName() {
        return "#text";
    }

    @Override public String nodeName() {
        return "#text";
    }

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
        HtmlTextNode tailNode = new HtmlTextNode(tail);
        if (parentNode != null)
            parentNode.addChildren(siblingIndex()+1, tailNode);

        return tailNode;
    }

    @Override
    public boolean isElementContentWhitespace() {
        return isBlank();
    }

    /**
     Get the (unencoded) text of this text node, including any newlines and spaces present in the original.
     @return text
     */
    @Override
    public String getWholeText() {
        return coreValue();
    }

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
     * Set the text content of this text node.
     * @param text unencoded text
     * @return this, for chaining
     */
    public HtmlTextNode text(String text) {
        coreValue(text);
        return this;
    }
}
