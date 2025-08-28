package zenzai.select;

import java.util.ArrayList;
import java.util.List;

import zenzai.nodes.Node;

public class Nodes<T extends Node> extends ArrayList<T> {
    public Nodes() {
    }

    public Nodes(int initialCapacity) {
        super(initialCapacity);
    }

    public Nodes(List<T> nodes) {
        super(nodes);
    }
}
