package zenzai.nodes;

import org.w3c.dom.DOMException;
import org.w3c.dom.Text;

public abstract class HtmlTextNode extends LeafNode implements Text {

    public abstract Text splitText(int offset) throws DOMException;
    public abstract boolean isElementContentWhitespace();
    public abstract String getWholeText();
    public abstract Text replaceWholeText(String content) throws DOMException;

    /**
     Create a new TextNode representing the supplied (unencoded) text).

     @param text raw text
     @see #createFromEncoded(String)
     */
    public HtmlTextNode(String text) {
        super(text);
    }

    @Override public String nodeName() {
        return "#text";
    }
}
