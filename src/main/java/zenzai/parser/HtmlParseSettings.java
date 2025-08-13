package zenzai.parser;

import org.jsoup.nodes.Attributes;

import static org.jsoup.internal.Normalizer.lowerCase;
import static org.jsoup.internal.Normalizer.normalize;

/**
 * Controls parser case settings, to optionally preserve tag and/or attribute name case.
 */
public class HtmlParseSettings {
    /**
     * HTML default settings: both tag and attribute names are lower-cased during parsing.
     */
    public static final HtmlParseSettings htmlDefault;
    /**
     * Preserve both tag and attribute case.
     */
    public static final HtmlParseSettings preserveCase;

    static {
        htmlDefault = new HtmlParseSettings(false, false);
        preserveCase = new HtmlParseSettings(true, true);
    }

    private final boolean preserveTagCase;
    private final boolean preserveAttributeCase;

    /**
     * Returns true if preserving tag name case.
     */
    public boolean preserveTagCase() {
        return preserveTagCase;
    }

    /**
     * Returns true if preserving attribute case.
     */
    public boolean preserveAttributeCase() {
        return preserveAttributeCase;
    }

    /**
     * Define parse settings.
     * @param tag preserve tag case?
     * @param attribute preserve attribute name case?
     */
    public HtmlParseSettings(boolean tag, boolean attribute) {
        preserveTagCase = tag;
        preserveAttributeCase = attribute;
    }

    HtmlParseSettings(HtmlParseSettings copy) {
        this(copy.preserveTagCase, copy.preserveAttributeCase);
    }

    /**
     * Normalizes a tag name according to the case preservation setting.
     */
    public String normalizeTag(String name) {
        name = name.trim();
        if (!preserveTagCase)
            name = lowerCase(name);
        return name;
    }

    /**
     * Normalizes an attribute according to the case preservation setting.
     */
    public String normalizeAttribute(String name) {
        name = name.trim();
        if (!preserveAttributeCase)
            name = lowerCase(name);
        return name;
    }

    void normalizeAttributes(Attributes attributes) {
        if (!preserveAttributeCase) {
            attributes.normalize();
        }
    }

    /** Returns the normal name that a Tag will have (trimmed and lower-cased) */
    static String normalName(String name) {
        return normalize(name);
    }
}
