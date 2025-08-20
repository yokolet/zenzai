package zenzai.nodes;

public class DataNode extends LeafNode {

    /**
     Create a new DataNode.
     @param data data contents
     */
    public DataNode(String data) {
        super(data);
    }

    @Override public String nodeName() {
        return "#data";
    }
}
