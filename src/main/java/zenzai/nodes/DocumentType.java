package zenzai.nodes;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import zenzai.helper.Validate;
import zenzai.internal.StringUtil;

public abstract class DocumentType extends LeafNode implements org.w3c.dom.DocumentType {
    public static final String PUBLIC_KEY = "PUBLIC";
    public static final String SYSTEM_KEY = "SYSTEM";
    private static final String NameKey = "name";
    private static final String PubSysKey = "pubSysKey"; // PUBLIC or SYSTEM
    private static final String PublicId = "publicId";
    private static final String SystemId = "systemId";

    public abstract String getName();
    public abstract NamedNodeMap getEntities();
    public abstract NamedNodeMap getNotations();
    public abstract String getPublicId();
    public abstract String getSystemId();
    public abstract String getInternalSubset();

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

    // org.w3d.dom.Node
    @Override
    public String getNodeValue() throws DOMException {
        return null;
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

    public void setPubSysKey(@Nullable String value) {
        if (value != null)
            attr(PubSysKey, value);
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
