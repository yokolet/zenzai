package zenzai.nodes;

import org.jspecify.annotations.Nullable;
import zenzai.helper.Validate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static zenzai.internal.SharedConstants.UserDataKey;
import static zenzai.internal.SharedConstants.AttrRangeKey;
import static zenzai.nodes.HtmlRange.AttributeRange.UntrackedAttr;

public abstract class HtmlAttributes implements Iterable<HtmlAttribute>, Cloneable {
    // Indicates an internal key. Can't be set via HTML. (It could be set via accessor, but not too worried about that. Suppressed from list, iter, size.)
    static final char InternalPrefix = '/';
    private static final String EmptyString = "";
    static final int NotFound = -1;
    // the number of instance fields is kept as low as possible giving an object size of 24 bytes
    // number of slots used (not total capacity, which is keys.length). Package visible for actual size (incl internal)
    int size = 0;
    // sampling found mean count when attrs present = 1.49; 1.08 overall. 2.6:1 don't have any attrs.
    private static final int InitialCapacity = 3;
    private static final int GrowthFactor = 2;
    // keys are not null, but contents may be. Same for vals
    @Nullable
    String[] keys = new String[InitialCapacity];
    // Genericish: all non-internal attribute values must be Strings and are cast on access.
    @Nullable
    Object[] vals = new Object[InitialCapacity];

    @Override
    public HtmlAttributes clone() {
        HtmlAttributes clone;
        try {
            clone = (HtmlAttributes) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        clone.size = size;
        clone.keys = Arrays.copyOf(keys, size);
        clone.vals = Arrays.copyOf(vals, size);

        // make a copy of the user data map. (Contents are shallow).
        int i = indexOfKey(UserDataKey);
        if (i != NotFound) {
            //noinspection unchecked
            vals[i] = new HashMap<>((Map<String, Object>) vals[i]);
        }

        return clone;
    }

    /**
     * Get an attribute value by key.
     *
     * @param key the (case-sensitive) attribute key
     * @return the attribute value if set; or empty string if not set (or a boolean attribute).
     * @see #hasKey(String)
     */
    public String get(String key) {
        int i = indexOfKey(key);
        return i == NotFound ? EmptyString : checkNotNull(vals[i]);
    }

    /**
     * Set a new attribute, or replace an existing one by key.
     * @param key case sensitive attribute key (not null)
     * @param value attribute value (which can be null, to set a true boolean attribute)
     * @return these attributes, for chaining
     */
    public HtmlAttributes put(String key, @Nullable String value) {
        Validate.notNull(key);
        int i = indexOfKey(key);
        if (i != NotFound)
            vals[i] = value;
        else
            addObject(key, value);
        return this;
    }

    /**
     * Adds a new attribute. Will produce duplicates if the key already exists.
     *
     * @see HtmlAttributes#put(String, String)
     */
    public HtmlAttributes add(String key, @Nullable String value) {
        addObject(key, value);
        return this;
    }

    /**
     Get the number of attributes in this set, excluding any internal-only attributes (e.g. user data).
     <p>Internal attributes are excluded from the {@link #html()}, {@link #asList()}, and {@link #iterator()}
     methods.</p>

     @return size
     */
    public int size() {
        if (size == 0) return 0;
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (!isInternalKey(keys[i]))  count++;
        }
        return count;
    }

    /**
     Get the source ranges (start to end position) in the original input source from which this attribute's <b>name</b>
     and <b>value</b> were parsed.
     <p>Position tracking must be enabled prior to parsing the content.</p>
     @param key the attribute name
     @return the ranges for the attribute's name and value, or {@code untracked} if the attribute does not exist or its range
     was not tracked.
     @see org.jsoup.parser.Parser#setTrackPosition(boolean)
     @see HtmlAttribute#sourceRange()
     @see HtmlNode#sourceRange()
     @see HtmlElement#endSourceRange()
     @since 1.17.1
     */
    public HtmlRange.AttributeRange sourceRange(String key) {
        if (!hasKey(key)) return UntrackedAttr;
        Map<String, HtmlRange.AttributeRange> ranges = getRanges();
        if (ranges == null) return HtmlRange.AttributeRange.UntrackedAttr;
        HtmlRange.AttributeRange range = ranges.get(key);
        return range != null ? range : HtmlRange.AttributeRange.UntrackedAttr;
    }

    /**
     Set the source ranges (start to end position) from which this attribute's <b>name</b> and <b>value</b> were parsed.
     @param key the attribute name
     @param range the range for the attribute's name and value
     @return these attributes, for chaining
     @since 1.18.2
     */
    public HtmlAttributes sourceRange(String key, HtmlRange.AttributeRange range) {
        Validate.notNull(key);
        Validate.notNull(range);
        Map<String, HtmlRange.AttributeRange> ranges = getRanges();
        if (ranges == null) {
            ranges = new HashMap<>();
            userData(AttrRangeKey, ranges);
        }
        ranges.put(key, range);
        return this;
    }

    /**
     Tests if these attributes contain an attribute with this key.
     @param key case-sensitive key to check for
     @return true if key exists, false otherwise
     */
    public boolean hasKey(String key) {
        return indexOfKey(key) != NotFound;
    }

    /**
     Get an arbitrary user-data object by key.
     * @param key case-sensitive key to the object.
     * @return the object associated to this key, or {@code null} if not found.
     * @see #userData(String key, Object val)
     * @since 1.17.1
     */
    @Nullable
    public Object userData(String key) {
        Validate.notNull(key);
        if (!hasUserData()) return null; // no user data exists
        Map<String, Object> userData = userData();
        return userData.get(key);
    }

    /**
     Set an arbitrary user-data object by key. Will be treated as an internal attribute, so will not be emitted in HTML.
     * @param key case-sensitive key
     * @param value object value. Providing a {@code null} value has the effect of removing the key from the userData map.
     * @return these attributes
     * @see #userData(String key)
     * @since 1.17.1
     */
    public HtmlAttributes userData(String key, @Nullable Object value) {
        Validate.notNull(key);
        if (value == null && !hasKey(UserDataKey)) return this; // no user data exists, so short-circuit
        Map<String, Object> userData = userData();
        if (value == null)  userData.remove(key);
        else                userData.put(key, value);
        return this;
    }

    /**
     Tests if these attributes contain an attribute with this key.
     @param key key to check for
     @return true if key exists, false otherwise
     */
    public boolean hasKeyIgnoreCase(String key) {
        return indexOfKeyIgnoreCase(key) != NotFound;
    }

    /**
     * Get an attribute's value by case-insensitive key
     * @param key the attribute name
     * @return the first matching attribute value if set; or empty string if not set (ora boolean attribute).
     */
    public String getIgnoreCase(String key) {
        int i = indexOfKeyIgnoreCase(key);
        return i == NotFound ? EmptyString : checkNotNull(vals[i]);
    }

    /** Get the Ranges, if tracking is enabled; null otherwise. */
    @Nullable Map<String, HtmlRange.AttributeRange> getRanges() {
        //noinspection unchecked
        return (Map<String, HtmlRange.AttributeRange>) userData(AttrRangeKey);
    }

    // we track boolean attributes as null in values - they're just keys. so returns empty for consumers
    // casts to String, so only for non-internal attributes
    static String checkNotNull(@Nullable Object val) {
        return val == null ? EmptyString : (String) val;
    }

    static boolean isInternalKey(String key) {
        return key.length() > 1 && key.charAt(0) == InternalPrefix;
    }

    int indexOfKey(String key) {
        Validate.notNull(key);
        for (int i = 0; i < size; i++) {
            if (key.equals(keys[i]))
                return i;
        }
        return NotFound;
    }

    /**
     Get the map holding any user-data associated with these Attributes. Will be created empty on first use. Held as
     an internal attribute, not a field member, to reduce the memory footprint of Attributes when not used. Can hold
     arbitrary objects; use for source ranges, connecting W3C nodes to Elements, etc.
     * @return the map holding user-data
     */
    Map<String, Object> userData() {
        final Map<String, Object> userData;
        int i = indexOfKey(UserDataKey);
        if (i == NotFound) {
            userData = new HashMap<>();
            addObject(UserDataKey, userData);
        } else {
            //noinspection unchecked
            userData = (Map<String, Object>) vals[i];
        }
        assert userData != null;
        return userData;
    }

    /**
     Check if these attributes have any user data associated with them.
     */
    boolean hasUserData() {
        return hasKey(UserDataKey);
    }

    static String internalKey(String key) {
        return InternalPrefix + key;
    }

    private void addObject(String key, @Nullable Object value) {
        checkCapacity(size + 1);
        keys[size] = key;
        vals[size] = value;
        size++;
    }

    // check there's room for more
    private void checkCapacity(int minNewSize) {
        Validate.isTrue(minNewSize >= size);
        int curCap = keys.length;
        if (curCap >= minNewSize)
            return;
        int newCap = curCap >= InitialCapacity ? size * GrowthFactor : InitialCapacity;
        if (minNewSize > newCap)
            newCap = minNewSize;

        keys = Arrays.copyOf(keys, newCap);
        vals = Arrays.copyOf(vals, newCap);
    }

    private int indexOfKeyIgnoreCase(String key) {
        Validate.notNull(key);
        for (int i = 0; i < size; i++) {
            if (key.equalsIgnoreCase(keys[i]))
                return i;
        }
        return NotFound;
    }
}
