package zenzai.nodes;

import org.jspecify.annotations.Nullable;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.TypeInfo;

public abstract class Attribute implements Cloneable, Attr {
    private String key;
    @Nullable private String val;
    @Nullable Attributes parent; // used to update the holding Attributes when the key / value is changed via this interface

    //public abstract String getName();
    public abstract boolean getSpecified();
    public abstract void setValue(String value) throws DOMException;
    public abstract Element getOnwerElement() throws DOMException;
    public abstract TypeInfo getSchemaTypeInfo();
    public abstract boolean isId();

    @Override
    public String getNodeName() {
        return getKey();
    }

    /**
     * Returns the name of this attribute. If Node.localName is different from null, this attribute is a qualified name.
     * @return the attribute name
     */
    @Override
    public String getName() {
        return getKey();
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
     @see HtmlElement#endSourceRange()
     @since 1.17.1
     */
    public Range.AttributeRange sourceRange() {
        if (parent == null) return Range.AttributeRange.UntrackedAttr;
        return parent.sourceRange(key);
    }
}
