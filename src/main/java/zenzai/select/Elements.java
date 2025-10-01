package zenzai.select;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import zenzai.internal.StringUtil;
import zenzai.nodes.Element;
import zenzai.nodes.FormElement;

public class Elements extends Nodes<Element> {
    public Elements() {
    }

    public Elements(int initialCapacity) {
        super(initialCapacity);
    }

    public Elements(List<Element> elements) {
        super(elements);
    }

    /**
     * Creates a deep copy of these elements.
     * @return a deep copy
     */
    @Override
    public Elements clone() {
        super.clone();
        Elements clone = new Elements(size());
        for (Element e : this)
            clone.add(e.clone());
        return clone;
    }

    /**
     * Update (rename) the tag name of each matched element. For example, to change each {@code <i>} to a {@code <em>}, do
     * {@code doc.select("i").tagName("em");}
     *
     * @param tagName the new tag name
     * @return this, for chaining
     * @see Element#tagName(String)
     */
    public Elements tagName(String tagName) {
        for (Element element : this) {
            element.tagName(tagName);
        }
        return this;
    }

    /**
     Get the first matched element.
     @return The first matched element, or <code>null</code> if contents is empty.
     */
    @Override
    public @Nullable Element first() {
        return super.first();
    }

    /**
     Get the last matched element.
     @return The last matched element, or <code>null</code> if contents is empty.
     */
    @Override
    public @Nullable Element last() {
        return super.last();
    }

    /**
     * Remove each matched element from the DOM. This is similar to setting the outer HTML of each element to nothing.
     * <p>The elements will still be retained in this list, in case further processing of them is desired.</p>
     * <p>
     * E.g. HTML: {@code <div><p>Hello</p> <p>there</p> <img /></div>}<br>
     * <code>doc.select("p").remove();</code><br>
     * HTML = {@code <div> <img /></div>}
     * <p>
     * Note that this method should not be used to clean user-submitted HTML; rather, use {@link org.jsoup.safety.Cleaner} to clean HTML.
     * @return this, for chaining
     * @see Element#empty()
     * @see #empty()
     * @see #clear()
     */
    @Override
    public Elements remove() {
        super.remove();
        return this;
    }

    /**
     * Empty (remove all child nodes from) each matched element. This is similar to setting the inner HTML of each
     * element to nothing.
     * <p>
     * E.g. HTML: {@code <div><p>Hello <b>there</b></p> <p>now</p></div>}<br>
     * <code>doc.select("p").empty();</code><br>
     * HTML = {@code <div><p></p> <p></p></div>}
     * @return this, for chaining
     * @see Element#empty()
     * @see #remove()
     */
    public Elements empty() {
        for (Element element : this) {
            element.empty();
        }
        return this;
    }

    /**
     * Get the {@link FormElement} forms from the selected elements, if any.
     * @return a list of {@link FormElement}s pulled from the matched elements. The list will be empty if the elements contain
     * no forms.
     */
    public List<FormElement> forms() {
        ArrayList<FormElement> forms = new ArrayList<>();
        for (Element el: this)
            if (el instanceof FormElement)
                forms.add((FormElement) el);
        return forms;
    }

    /**
     * Get the combined text of all the matched elements.
     * <p>
     * Note that it is possible to get repeats if the matched elements contain both parent elements and their own
     * children, as the Element.text() method returns the combined text of a parent and all its children.
     * @return string of all text: unescaped and no HTML.
     * @see Element#text()
     * @see #eachText()
     */
    public String text() {
        return stream()
                .map(Element::text)
                .collect(StringUtil.joining(" "));
    }

    /**
     * Get the combined inner HTML of all matched elements.
     * @return string of all element's inner HTML.
     * @see #text()
     * @see #outerHtml()
     */
    public String html() {
        return stream()
                .map(Element::html)
                .collect(StringUtil.joining("\n"));
    }
}
