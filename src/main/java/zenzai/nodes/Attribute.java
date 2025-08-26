package zenzai.nodes;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.TypeInfo;

import zenzai.helper.Validate;
import zenzai.internal.Normalizer;
import zenzai.internal.QuietAppendable;
import zenzai.nodes.Document.OutputSettings.Syntax;

public abstract class Attribute implements Cloneable, Attr {
    private static final String[] booleanAttributes = {
            "allowfullscreen", "async", "autofocus", "checked", "compact", "declare", "default", "defer", "disabled",
            "formnovalidate", "hidden", "inert", "ismap", "itemscope", "multiple", "muted", "nohref", "noresize",
            "noshade", "novalidate", "nowrap", "open", "readonly", "required", "reversed", "seamless", "selected",
            "sortable", "truespeed", "typemustmatch"
    };
    private String key;
    @Nullable private String val;
    @Nullable Attributes parent; // used to update the holding Attributes when the key / value is changed via this interface

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
    @Override
    public void setValue(String value) throws DOMException {
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
        return getKey().toLowerCase().endsWith("id");
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
     Get the attribute's key (aka name).
     @return the attribute key
     */
    public String getKey() {
        return key;
    }

    /**
     Get the attribute value. Will return an empty string if the value is not set.
     @return the attribute value
     */
    public String getValue() {
        return Attributes.checkNotNull(val);
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
     Get the source ranges (start to end positions) in the original input source from which this attribute's <b>name</b>
     and <b>value</b> were parsed.
     <p>Position tracking must be enabled prior to parsing the content.</p>
     @return the ranges for the attribute's name and value, or {@code untracked} if the attribute does not exist or its range
     was not tracked.
     @see org.jsoup.parser.Parser#setTrackPosition(boolean)
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
