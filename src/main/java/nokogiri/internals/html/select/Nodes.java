package nokogiri.internals.html.select;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import nokogiri.internals.html.internal.StringUtil;
import nokogiri.internals.html.nodes.Element;
import nokogiri.internals.html.nodes.Node;

public class Nodes<T extends Node> extends ArrayList<T> implements org.w3c.dom.NodeList  {
    private static final long serialVersionUID = 1L;

    public Nodes() {
    }

    public Nodes(int initialCapacity) {
        super(initialCapacity);
    }

    public Nodes(List<T> nodes) {
        super(nodes);
    }

    /**
     * Creates a deep copy of these nodes.
     * @return a deep copy
     */
    @Override
    public Nodes<T> clone() {
        super.clone();
        Nodes<T> clone = new Nodes<>(size());
        for (T node : this) {
            @SuppressWarnings("unchecked")
            T clonedNode = (T) node.clone();
            clone.add(clonedNode);
        }
        return clone;
    }

    // org.w3c.dom.NodeList
    @Override
    public org.w3c.dom.Node item(int index) {
        return get(index);
    }

    // org.w3c.dom.NodeList
    @Override
    public int getLength() {
        return size();
    }

    // list-like methods
    /**
     Get the first matched element.
     @return The first matched element, or <code>null</code> if contents is empty.
     */
    public @Nullable T first() {
        return isEmpty() ? null : get(0);
    }

    /**
     Get the last matched element.
     @return The last matched element, or <code>null</code> if contents is empty.
     */
    public @Nullable T last() {
        return isEmpty() ? null : get(size() - 1);
    }

    /**
     Remove each matched node from the DOM.
     <p>The nodes will still be retained in this list, in case further processing of them is desired.</p>
     <p>
     E.g. HTML: {@code <div><p>Hello</p> <p>there</p> <img></div>}<br>
     <code>doc.select("p").remove();</code><br>
     HTML = {@code <div> <img></div>}
     <p>
     Note that this method should not be used to clean user-submitted HTML; rather, use {@link org.jsoup.safety.Cleaner}
     to clean HTML.

     @return this, for chaining
     @see Element#empty()
     @see Elements#empty()
     @see #clear()
     */
    public Nodes<T> remove() {
        for (T node : this) {
            node.remove();
        }
        return this;
    }

    /**
     Get the combined outer HTML of all matched nodes.

     @return string of all node's outer HTML.
     @see Elements#text()
     @see Elements#html()
     */
    public String outerHtml() {
        return stream()
                .map(Node::outerHtml)
                .collect(StringUtil.joining("\n"));
    }
}
