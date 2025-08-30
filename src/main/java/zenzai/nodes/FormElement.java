package zenzai.nodes;

import org.jspecify.annotations.Nullable;
import zenzai.parser.Tag;
import zenzai.select.Elements;

public abstract class FormElement extends Element {
    private final Elements linkedEls = new Elements();

    /**
     * Create a new, standalone form element.
     *
     * @param tag        tag of this element
     * @param baseUri    the base URI
     * @param attributes initial attributes
     */
    public FormElement(Tag tag, @Nullable String baseUri, @Nullable Attributes attributes) {
        super(tag, baseUri, attributes);
    }

    @Override
    public FormElement clone() {
        return (FormElement) super.clone();
    }

    @Override
    protected void removeChild(zenzai.nodes.Node out) {
        super.removeChild(out);
        linkedEls.remove(out);
    }

    /**
     * Add a form control element to this form.
     * @param element form control to add
     * @return this form element, for chaining
     */
    public FormElement addElement(Element element) {
        linkedEls.add(element);
        return this;
    }
}
