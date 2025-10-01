package zenzai.nodes;

import org.jspecify.annotations.Nullable;

import zenzai.internal.SharedConstants;
import zenzai.internal.StringUtil;
import zenzai.parser.Tag;
import zenzai.select.Elements;
import zenzai.select.Evaluator;
import zenzai.select.Selector;

public class FormElement extends Element {
    private final Elements linkedEls = new Elements();
    private static final Evaluator submittable = Selector.evaluatorOf(StringUtil.join(SharedConstants.FormSubmitTags, ", "));

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

    /**
     * Get the list of form control elements associated with this form.
     * @return form controls associated with this element.
     */
    public Elements elements() {
        // As elements may have been added or removed from the DOM after parse, prepare a new list that unions them:
        Elements els = select(submittable); // current form children
        for (Element linkedEl : linkedEls) {
            if (linkedEl.ownerDocument() != null && !els.contains(linkedEl)) {
                els.add(linkedEl); // adds previously linked elements, that weren't previously removed from the DOM
            }
        }
        return els;
    }
}
