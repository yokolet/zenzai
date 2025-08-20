package zenzai.nodes;

import org.w3c.dom.Comment;

public abstract class HtmlComment extends LeafNode implements Comment {

    /**
     Create a new comment node.
     @param data The contents of the comment
     */
    public HtmlComment(String data) {
        super(data);
    }

    @Override public String nodeName() {
        return "#comment";
    }
}
