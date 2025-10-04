package nokogiri.internals.html.nodes;

import org.w3c.dom.DOMException;

import nokogiri.internals.html.internal.QuietAppendable;
import nokogiri.internals.html.internal.StringUtil;

public class XmlDeclaration extends LeafNode {
    /**
     First char is `!` if isDeclaration, like in {@code  <!ENTITY ...>}.
     Otherwise, is `?`, a processing instruction, like {@code <?xml .... ?>} (and note trailing `?`).
     */
    private final boolean isDeclaration;

    /**
     * Create a new XML declaration
     * @param name of declaration
     * @param isDeclaration {@code true} if a declaration (first char is `!`), otherwise a processing instruction (first char is `?`).
     */
    public XmlDeclaration(String name, boolean isDeclaration) {
        super(name);
        this.isDeclaration = isDeclaration;
    }

    @Override public String nodeName() {
        return "#declaration";
    }

    // org.w3c.dom.Node
    @Override
    public String getNodeName() {
        return nodeName();
    }

    // org.w3d.dom.Node
    @Override
    public String getNodeValue() throws DOMException {
        // TODO: check if a string length limit exists
        // DOMException.DOMSTRING_SIZE_ERR is raised when it would return more characters than fit in
        // a DOMString variable on the implementation platform. It depends on a web browser implementation.
        return null;
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
    public String getTextContent() throws DOMException {
        return coreValue();
    }

    // org.w3c.dom.Node
    @Override
    public void setTextContent(String textContent) throws DOMException {
        coreValue(textContent);
    }

    /**
     * Get the name of this declaration.
     * @return name of this declaration.
     */
    public String name() {
        return coreValue();
    }

    /**
     * Get the unencoded XML declaration.
     * @return XML declaration
     */
    public String getWholeDeclaration() {
        StringBuilder sb = StringUtil.borrowBuilder();
        getWholeDeclaration(QuietAppendable.wrap(sb), new Document.OutputSettings());
        return StringUtil.releaseBuilder(sb).trim();
    }

    private void getWholeDeclaration(QuietAppendable accum, Document.OutputSettings out) {
        for (Attribute attribute : attributes()) {
            String key = attribute.getKey();
            String val = attribute.getValue();
            if (!key.equals(nodeName())) { // skips coreValue (name)
                accum.append(' ');
                // basically like Attribute, but skip empty vals in XML
                accum.append(key);
                if (!val.isEmpty()) {
                    accum.append("=\"");
                    Entities.escape(accum, val, out, Entities.ForAttribute);
                    accum.append('"');
                }
            }
        }
    }

    @Override
    void outerHtmlHead(QuietAppendable accum, Document.OutputSettings out) {
        accum
                .append("<")
                .append(isDeclaration ? "!" : "?")
                .append(coreValue());
        getWholeDeclaration(accum, out);
        accum
                .append(isDeclaration ? "" : "?")
                .append(">");
    }

    @Override
    void outerHtmlTail(QuietAppendable accum, Document.OutputSettings out) {
    }

    @Override
    public String toString() {
        return outerHtml();
    }

    @Override
    public nokogiri.internals.html.nodes.XmlDeclaration clone() {
        return (nokogiri.internals.html.nodes.XmlDeclaration) super.clone();
    }
}
