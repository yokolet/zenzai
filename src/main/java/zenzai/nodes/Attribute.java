package zenzai.nodes;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.w3c.dom.*;

import org.w3c.dom.Element;
import zenzai.helper.Validate;
import zenzai.helper.W3CValidation;
import zenzai.internal.Normalizer;
import zenzai.internal.QuietAppendable;
import zenzai.internal.SharedConstants;
import zenzai.internal.StringUtil;
import zenzai.nodes.Document.OutputSettings.Syntax;
import zenzai.parser.Parser;

// An attribute can have children if those are Text and EntityReference.
// However, HTML attributes cannot contain children
public class Attribute implements Cloneable, Attr {
    private static final String[] booleanAttributes = {
            "allowfullscreen", "async", "autofocus", "checked", "compact", "declare", "default", "defer", "disabled",
            "formnovalidate", "hidden", "inert", "ismap", "itemscope", "multiple", "muted", "nohref", "noresize",
            "noshade", "novalidate", "nowrap", "open", "readonly", "required", "reversed", "seamless", "selected",
            "sortable", "truespeed", "typemustmatch"
    };
    private String key;
    @Nullable private String val;
    @Nullable Attributes parent; // used to update the holding Attributes when the key / value is changed via this interface

    /**
     * Create a new attribute from unencoded (raw) key and value.
     * @param key attribute key; case is preserved.
     * @param value attribute value (may be null)
     * @see #createFromEncoded
     */
    public Attribute(String key, @Nullable String value) {
        this(key, value, null);
    }

    /**
     * Create a new attribute from unencoded (raw) key and value.
     * @param key attribute key; case is preserved.
     * @param val attribute value (may be null)
     * @param parent the containing Attributes (this Attribute is not automatically added to said Attributes)
     * @see #createFromEncoded*/
    public Attribute(String key, @Nullable String val, @Nullable Attributes parent) {
        Validate.notNull(key);
        key = key.trim();
        Validate.notEmpty(key); // trimming could potentially make empty, so validate here
        this.key = key;
        this.val = val;
        this.parent = parent;
    }

    // org.w3c.dom.Node
    @Override
    public String getNodeName() {
        return getKey();
    }

    // org.w3d.dom.Node
    @Override
    public String getNodeValue() throws DOMException {
        // DOMException.DOMSTRING_SIZE_ERR should be raised when an attribute value is very long
        // such that over 10 million.
        return getValue();
    }

    // org.w3c.dom.Node
    @Override
    public void setNodeValue(String value) throws DOMException {
        // NO_MODIFICATION_ALLOWED_ERR will be raised when the node is readonly and if not defined to be null.
        setValueWithReturn(value);
    }

    // org.w3c.dom.Node
    @Override
    public short getNodeType() {
        return Node.ATTRIBUTE_NODE;
    }

    // org.w3c.dom.Node
    @Override
    public org.w3c.dom.Node getPreviousSibling() { return null; }

    // org.w3c.dom.Node
    @Override
    public org.w3c.dom.Node getNextSibling() { return null; }

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

    // org.w3c.dom.Node
    @Override
    public org.w3c.dom.Node getParentNode() { return parent.ownerElement; }

    // org.w3c.dom.Node
    @Override
    public org.w3c.dom.NodeList getChildNodes() { return new zenzai.nodes.Element.NodeList(0); }

    // org.w3c.dom.Node
    @Override
    public org.w3c.dom.Node getFirstChild() { return null; }
    // org.w3c.dom.Node
    @Override
    public org.w3c.dom.Node getLastChild() { return null; }
    // org.w3c.dom.Node
    @Override
    public NamedNodeMap getAttributes() { return null; }
    // org.w3c.dom.Node
    @Override
    public Document getOwnerDocument() { return parent != null ? parent.ownerElement.getOwnerDocument() : null; }
    // org.w3c.dom.Node
    @Override
    public org.w3c.dom.Node insertBefore(org.w3c.dom.Node newChild, org.w3c.dom.Node refChild) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported operation");
    }
    // org.w3c.dom.Node
    public org.w3c.dom.Node replaceChild(org.w3c.dom.Node newChild, org.w3c.dom.Node oldChild) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported operation");
    }
    // org.w3c.doom.Node
    @Override
    public org.w3c.dom.Node removeChild(org.w3c.dom.Node oldChild) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported operation");
    }
    // org.w3c.dom.Node
    @Override
    public org.w3c.dom.Node appendChild(org.w3c.dom.Node newChild) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported operation");
    }
    // org.w3c.dom.Node
    @Override
    public boolean hasChildNodes() { return false; }
    // org.w3c.dom.Node
    @Override
    public org.w3c.dom.Node cloneNode(boolean deep) { return clone(); }
    // org.w3c.dom.Node
    @Override
    public void normalize() {
        // TODO: implement this
    }
    // org.w3c.dom.Node
    @Override
    public boolean isSupported(String feature, String version) { return false; }
    // org.w3c.dom.Node
    @Override
    public String getNamespaceURI() { return namespace(); }
    // org.w3c.dom.Node
    @Override
    public String getPrefix() { return prefix(); }
    // org.w3c.dom.Node
    @Override
    public void setPrefix(String prefix) throws DOMException {
        // no-op
    }
    // org.w3c.dom.Node
    @Override
    public String getLocalName() { return localName(); }
    // org.w3c.dom.Node
    @Override
    public boolean hasAttributes() { return false; }
    // org.w3c.dom.Node
    @Override
    public String getBaseURI() { return null; }
    // org.w3c.dom.Node
    @Override
    public short compareDocumentPosition(org.w3c.dom.Node other) throws DOMException {
        // TODO: decide if the implementation of this method is required
        // see: https://developer.mozilla.org/en-US/docs/Web/API/Node/compareDocumentPosition
        return 0;
    }
    // org.w3c.dom.Node
    @Override
    public boolean isSameNode(org.w3c.dom.Node other) {
        return this.hashCode() == other.hashCode();
    }
    // org.w3c.dom.Node
    @Override
    public String lookupPrefix(String namespaceURI) { return null; }
    // org.w3c.dom.Node
    @Override
    public boolean isDefaultNamespace(String namespaceURI) { return namespaceURI.equals(Parser.NamespaceHtml); }
    @Override
    public String lookupNamespaceURI(String prefix) { return null; }
    // org.w3c.dom.Node
    @Override
    public boolean isEqualNode(org.w3c.dom.Node arg) {
        // TODO: check correctness of this method
        if (getNodeType() != arg.getNodeType()) return false;
        if (!getNodeName().equals(arg.getNodeName())) return false;
        if (!getLocalName().equals(arg.getLocalName())) return false;
        if (!getNodeValue().equals(arg.getNodeValue())) return false;
        if (!getAttributes().equals(arg.getAttributes())) return false;
        return true;
    }
    // org.w3c.dom.Node
    @Override
    public Object getFeature(String feature, String version) { return null; }
    // org.w3c.dom.Node
    @Override
    public Object setUserData(String key, Object data, UserDataHandler handler) {
        // TODO: guess HTML5 doesn't have user data feature
        return null;
    }
    // org.w3c.dom.Node
    @Override
    public Object getUserData(String key) {
        // TODO: same as above. HTML5 doesn't have user data feature
        return null;
    }

    /**
     * Returns the name of this attribute. If Node.localName is different from null, this attribute is a qualified name.
     * @return the attribute name
     */
    // org.w3c.dom.Attr
    @Override
    public String getName() {
        return getKey();
    }

    /**
     * True if this attribute was explicitly given a value in the instance document, false otherwise.
     * If the application changed the value of this attribute node (even if it ends up having the same value as
     * the default value) then it is set to true. The implementation may handle attributes with default values
     * from other schemas similarly but applications should use Document.normalizeDocument() to guarantee
     * this information is up-to-date.
     */
    // org.w3c.dom.Attr
    @Override
    public boolean getSpecified() {
        // TODO: verify the behavior
        return zenzai.nodes.Attribute.isBooleanAttribute(getKey());
    }

    // org.w3c.dom.Attr
    // zenzai.nodes.Attribute
    /**
     Get the attribute value. Will return an empty string if the value is not set.
     @return the attribute value
     */
    @Override
    public String getValue() {
        return Attributes.checkNotNull(val);
    }

    // org.w3c.dom.Attr
    @Override
    public void setValue(String value) throws DOMException {
        W3CValidation.modificationAllowed(parent.ownerElement);
        setValueWithReturn(value);
    }

    // org.w3c.dom.Attr
    @Override
    public Element getOwnerElement() {
        return parent.ownerElement;
    }

    // org.w3c.dom.Attr
    @Override
    public TypeInfo getSchemaTypeInfo() {
        // TODO: check what should be returned
        return null;
    }

    // org.w3c.dom.Attr
    @Override
    public boolean isId() {
        return getKey().equalsIgnoreCase("id");
    }

    /**
     Get the string representation of this attribute, implemented as {@link #html()}.
     @return string
     */
    @Override
    public String toString() {
        return html();
    }

    @Override
    public boolean equals(@Nullable Object o) { // note parent not considered
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attribute attribute = (Attribute) o;
        return Objects.equals(key, attribute.key) && Objects.equals(val, attribute.val);
    }

    @Override
    public int hashCode() { // note parent not considered
        return Objects.hash(key, val);
    }

    @Override
    public Attribute clone() {
        try {
            return (Attribute) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     Get the attribute's key (aka name).
     @return the attribute key
     */
    public String getKey() {
        return key;
    }

    /**
     Set the attribute value.
     @param val the new attribute value; may be null (to set an enabled boolean attribute)
     @return the previous value (if was null; an empty string)
     */
    public String setValueWithReturn(@Nullable String val) {
        String oldVal = this.val;
        if (parent != null) {
            int i = parent.indexOfKey(this.key);
            if (i != Attributes.NotFound) {
                oldVal = parent.get(this.key); // trust the container more
                parent.vals[i] = val;
            }
        }
        this.val = val;
        return Attributes.checkNotNull(oldVal);
    }

    /**
     * Check if this Attribute has a value. Set boolean attributes have no value.
     * @return if this is a boolean attribute / attribute without a value
     */
    public boolean hasDeclaredValue() {
        return val != null;
    }

    /**
     Get this attribute's key prefix, if it has one; else the empty string.
     <p>For example, the attribute {@code og:title} has prefix {@code og}, and local {@code title}.</p>

     @return the tag's prefix
     @since 1.20.1
     */
    public String prefix() {
        int pos = key.indexOf(':');
        if (pos == -1) return "";
        else return key.substring(0, pos);
    }

    /**
     Get this attribute's local name. The local name is the name without the prefix (if any).
     <p>For example, the attribute key {@code og:title} has local name {@code title}.</p>

     @return the tag's local name
     @since 1.20.1
     */
    public String localName() {
        int pos = key.indexOf(':');
        if (pos == -1) return key;
        else return key.substring(pos + 1);
    }

    /**
     Get this attribute's namespace URI, if the attribute was prefixed with a defined namespace name. Otherwise, returns
     the empty string. These will only be defined if using the XML parser.
     @return the tag's namespace URI, or empty string if not defined
     @since 1.20.1
     */
    public String namespace() {
        // set as el.attributes.userData(SharedConstants.XmlnsAttr + prefix, ns)
        if (parent != null) {
            String ns = (String) parent.userData(SharedConstants.XmlnsAttr + prefix());
            if (ns != null)
                return ns;
        }
        return "";
    }

    /**
     Get the HTML representation of this attribute; e.g. {@code href="index.html"}.
     @return HTML
     */
    public String html() {
        StringBuilder sb = StringUtil.borrowBuilder();
        html(QuietAppendable.wrap(sb), new Document.OutputSettings());
        return StringUtil.releaseBuilder(sb);
    }

    private static final Pattern xmlKeyReplace = Pattern.compile("[^-a-zA-Z0-9_:.]+");
    private static final Pattern htmlKeyReplace = Pattern.compile("[\\x00-\\x1f\\x7f-\\x9f \"'/=]+");

    /**
     * Get a valid attribute key for the given syntax. If the key is not valid, it will be coerced into a valid key.
     * @param key the original attribute key
     * @param syntax HTML or XML
     * @return the original key if it's valid; a key with invalid characters replaced with "_" otherwise; or null if a valid key could not be created.
     */
    @Nullable public static String getValidKey(String key, zenzai.nodes.Document.OutputSettings.Syntax syntax) {
        if (syntax == zenzai.nodes.Document.OutputSettings.Syntax.xml && !isValidXmlKey(key)) {
            key = xmlKeyReplace.matcher(key).replaceAll("_");
            return isValidXmlKey(key) ? key : null; // null if could not be coerced
        }
        else if (syntax == zenzai.nodes.Document.OutputSettings.Syntax.html && !isValidHtmlKey(key)) {
            key = htmlKeyReplace.matcher(key).replaceAll("_");
            return isValidHtmlKey(key) ? key : null; // null if could not be coerced
        }
        return key;
    }

    /**
     Set the attribute key; case is preserved.
     @param key the new key; must not be null
     */
    public void setKey(String key) {
        Validate.notNull(key);
        key = key.trim();
        Validate.notEmpty(key); // trimming could potentially make empty, so validate here
        if (parent != null) {
            int i = parent.indexOfKey(this.key);
            if (i != Attributes.NotFound) {
                String oldKey = parent.keys[i];
                parent.keys[i] = key;

                // if tracking source positions, update the key in the range map
                Map<String, Range.AttributeRange> ranges = parent.getRanges();
                if (ranges != null) {
                    Range.AttributeRange range = ranges.remove(oldKey);
                    ranges.put(key, range);
                }
            }
        }
        this.key = key;
    }

    /**
     Get the source ranges (start to end positions) in the original input source from which this attribute's <b>name</b>
     and <b>value</b> were parsed.
     <p>Position tracking must be enabled prior to parsing the content.</p>
     @return the ranges for the attribute's name and value, or {@code untracked} if the attribute does not exist or its range
     was not tracked.
     @see zenzai.parser.Parser#setTrackPosition(boolean)
     @see Attributes#sourceRange(String)
     @see zenzai.nodes.Node#sourceRange()
     @see zenzai.nodes.Element#endSourceRange()
     @since 1.17.1
     */
    public Range.AttributeRange sourceRange() {
        if (parent == null) return Range.AttributeRange.UntrackedAttr;
        return parent.sourceRange(key);
    }

    /**
     * Checks if this attribute name is defined as a boolean attribute in HTML5
     */
    public static boolean isBooleanAttribute(final String key) {
        return Arrays.binarySearch(booleanAttributes, Normalizer.lowerCase(key)) >= 0;
    }

    /**
     * Create a new Attribute from an unencoded key and a HTML attribute encoded value.
     * @param unencodedKey assumes the key is not encoded, as can be only run of simple \w chars.
     * @param encodedValue HTML attribute encoded value
     * @return attribute
     */
    public static Attribute createFromEncoded(String unencodedKey, String encodedValue) {
        String value = Entities.unescape(encodedValue, true);
        return new Attribute(unencodedKey, value, null); // parent will get set when Put
    }

    void html(QuietAppendable accum, Document.OutputSettings out) {
        html(key, val, accum, out);
    }

    static void html(String key, @Nullable String val, QuietAppendable accum, Document.OutputSettings out) {
        key = getValidKey(key, out.syntax());
        if (key == null) return; // can't write it :(
        htmlNoValidate(key, val, accum, out);
    }

    static void htmlNoValidate(String key, @Nullable String val, QuietAppendable accum, Document.OutputSettings out) {
        // structured like this so that Attributes can check we can write first, so it can add whitespace correctly
        accum.append(key);
        if (!shouldCollapseAttribute(key, val, out)) {
            accum.append("=\"");
            Entities.escape(accum, Attributes.checkNotNull(val), out, Entities.ForAttribute); // preserves whitespace
            accum.append('"');
        }
    }

    // collapse unknown foo=null, known checked=null, checked="", checked=checked; write out others
    protected static boolean shouldCollapseAttribute(final String key, @Nullable final String val, final zenzai.nodes.Document.OutputSettings out) {
        return (out.syntax() == Syntax.html &&
                (val == null || (val.isEmpty() || val.equalsIgnoreCase(key)) && Attribute.isBooleanAttribute(key)));
    }

    protected boolean isDataAttribute() {
        return isDataAttribute(key);
    }

    protected static boolean isDataAttribute(String key) {
        return key.startsWith(Attributes.dataPrefix) && key.length() > Attributes.dataPrefix.length();
    }

    private static boolean isValidXmlKey(String key) {
        // =~ [a-zA-Z_:][-a-zA-Z0-9_:.]*
        final int length = key.length();
        if (length == 0) return false;
        char c = key.charAt(0);
        if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == ':'))
            return false;
        for (int i = 1; i < length; i++) {
            c = key.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '-' || c == '_' || c == ':' || c == '.'))
                return false;
        }
        return true;
    }

    private static boolean isValidHtmlKey(String key) {
        // =~ [\x00-\x1f\x7f-\x9f "'/=]+
        final int length = key.length();
        if (length == 0) return false;
        for (int i = 0; i < length; i++) {
            char c = key.charAt(i);
            if ((c <= 0x1f) || (c >= 0x7f && c <= 0x9f) || c == ' ' || c == '"' || c == '\'' || c == '/' || c == '=')
                return false;
        }
        return true;
    }
}
