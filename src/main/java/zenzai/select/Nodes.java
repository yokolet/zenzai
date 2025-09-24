package zenzai.select;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import zenzai.nodes.Element;
import zenzai.nodes.Node;

public class Nodes<T extends Node> extends ArrayList<T> implements org.w3c.dom.NodeList  {
    public Nodes() {
    }

    public Nodes(int initialCapacity) {
        super(initialCapacity);
    }

    public Nodes(List<T> nodes) {
        super(nodes);
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
}
