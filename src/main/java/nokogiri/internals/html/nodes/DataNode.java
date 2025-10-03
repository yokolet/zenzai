package nokogiri.internals.html.nodes;

import org.w3c.dom.CharacterData;
import org.w3c.dom.DOMException;

import nokogiri.internals.html.helper.W3CValidation;
import nokogiri.internals.html.internal.QuietAppendable;

public class DataNode extends LeafNode implements CharacterData {

    /**
     Create a new DataNode.
     @param data data contents
     */
    public DataNode(String data) {
        super(data);
    }

    @Override
    public DataNode clone() {
        return (DataNode) super.clone();
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
        return getWholeData();
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

    // jsoup.nodes.Node
    @Override public String nodeName() {
        return "#text";
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

    /**
     Get the data contents of this node. Will be unescaped and with original new lines, space etc.
     @return data
     */
    public String getWholeData() {
        return coreValue();
    }

    /**
     * Set the data contents of this node.
     * @param data un-encoded data
     * @return this node, for chaining
     */
    public DataNode setWholeData(String data) {
        coreValue(data);
        return this;
    }

    @Override
    void outerHtmlHead(QuietAppendable accum, Document.OutputSettings out) {
        /* For XML output, escape the DataNode in a CData section. The data may contain pseudo-CData content if it was
        parsed as HTML, so don't double up Cdata. Output in polyglot HTML / XHTML / XML format. */
        final String data = getWholeData();
        if (out.syntax() == Document.OutputSettings.Syntax.xml && !data.contains("<![CDATA[")) {
            if (parentNameIs("script"))
                accum.append("//<![CDATA[\n").append(data).append("\n//]]>");
            else if (parentNameIs("style"))
                accum.append("/*<![CDATA[*/\n").append(data).append("\n/*]]>*/");
            else
                accum.append("<![CDATA[").append(data).append("]]>");
        } else {
            // In HTML, data is not escaped in the output of data nodes, so < and & in script, style is OK
            accum.append(data);
        }
    }
}
