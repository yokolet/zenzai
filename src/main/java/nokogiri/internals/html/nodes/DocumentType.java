package nokogiri.internals.html.nodes;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import nokogiri.internals.html.helper.Validate;
import nokogiri.internals.html.internal.QuietAppendable;
import nokogiri.internals.html.internal.StringUtil;
import nokogiri.internals.html.nodes.Document.OutputSettings.Syntax;

// From a DOM perspective, Document Type is not a leaf node. It can have children such as entity or notation.
// However, HTML Document Type <!DOCTYPE html> cannot have children.
public class DocumentType extends LeafNode implements org.w3c.dom.DocumentType {
    public static final String PUBLIC_KEY = "PUBLIC";
    public static final String SYSTEM_KEY = "SYSTEM";
    private static final String NameKey = "name";
    private static final String PubSysKey = "pubSysKey"; // PUBLIC or SYSTEM
    private static final String PublicId = "publicId";
    private static final String SystemId = "systemId";

    /**
     * Create a new doctype element.
     * @param name the doctype's name
     * @param publicId the doctype's public ID
     * @param systemId the doctype's system ID
     */
    public DocumentType(String name, String publicId, String systemId) {
        super(name);
        Validate.notNull(publicId);
        Validate.notNull(systemId);
        attributes()
                .add(NameKey, name)
                .add(PublicId, publicId)
                .add(SystemId, systemId);
        updatePubSyskey();
    }

    // org.w3c.dom.Node
    @Override
    public String getNodeName() {
        return attr(NameKey);
    }

    // org.w3c.dom.Node
    @Override
    public short getNodeType() {
        return Node.DOCUMENT_TYPE_NODE;
    }

    // org.w3c.dom.Node
    @Override
    public NamedNodeMap getAttributes() { return null; }

    // org.w3c.dom.Node
    @Override
    public String getTextContent() throws DOMException {
        return null;
    }

    // org.w3c.dom.Node
    @Override
    public void setTextContent(String textContent) throws DOMException {
        // no-op
    }

    // org.w3c.dom.DocumentType
    @Override
    public String getName() { return name(); }
    public NamedNodeMap getEntities() {
        // TODO: create Entity class and implement this method
        return null;
    }
    public NamedNodeMap getNotations() {
        // TODO: create Notation class and implement this method
        return null;
    }
    // org.w3c.dom.DocumentType
    @Override
    public String getPublicId() { return publicId(); }
    // org.w3c.dom.DocumentType
    @Override
    public String getSystemId() { return systemId(); }
    // org.w3c.dom.DocumentType
    @Override
    public String getInternalSubset() { return null;}

    @Override
    public String nodeName() {
        return "#doctype";
    }

    /**
     * Get this doctype's name (when set, or empty string)
     * @return doctype name
     */
    public String name() {
        return attr(NameKey);
    }

    /**
     * Get this doctype's Public ID (when set, or empty string)
     * @return doctype Public ID
     */
    public String publicId() {
        return attr(PublicId);
    }

    /**
     * Get this doctype's System ID (when set, or empty string)
     * @return doctype System ID
     */
    public String systemId() {
        return attr(SystemId);
    }

    public void setPubSysKey(@Nullable String value) {
        if (value != null)
            attr(PubSysKey, value);
    }

    @Override
    void outerHtmlHead(QuietAppendable accum, Document.OutputSettings out) {
        if (out.syntax() == Syntax.html && !has(PublicId) && !has(SystemId)) {
            // looks like a html5 doctype, go lowercase for aesthetics
            accum.append("<!doctype");
        } else {
            accum.append("<!DOCTYPE");
        }
        if (has(NameKey))
            accum.append(" ").append(attr(NameKey));
        if (has(PubSysKey))
            accum.append(" ").append(attr(PubSysKey));
        if (has(PublicId))
            accum.append(" \"").append(attr(PublicId)).append('"');
        if (has(SystemId))
            accum.append(" \"").append(attr(SystemId)).append('"');
        accum.append('>');
    }

    private void updatePubSyskey() {
        if (has(PublicId)) {
            attributes().add(PubSysKey, PUBLIC_KEY);
        } else if (has(SystemId))
            attributes().add(PubSysKey, SYSTEM_KEY);
    }

    private boolean has(final String attribute) {
        return !StringUtil.isBlank(attr(attribute));
    }
}
