package zenzai.select;

import java.util.ArrayList;
import java.util.List;

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
}
