package zenzai.nodes;

import org.jspecify.annotations.Nullable;
import zenzai.parser.Tag;
import zenzai.select.Elements;

public abstract class HtmlFormElement extends HtmlElement{
    private final Elements linkedEls = new Elements();

    /**
     * Create a new, standalone form element.
     *
     * @param tag        tag of this element
     * @param baseUri    the base URI
     * @param attributes initial attributes
     */
    public HtmlFormElement(Tag tag, @Nullable String baseUri, @Nullable Attributes attributes) {
        super(tag, baseUri, attributes);
    }

    /**
     * Add a form control element to this form.
     * @param element form control to add
     * @return this form element, for chaining
     */
    public HtmlFormElement addElement(HtmlElement element) {
        linkedEls.add(element);
        return this;
    }
}
