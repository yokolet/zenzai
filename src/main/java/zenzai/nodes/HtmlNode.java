package zenzai.nodes;

import java.util.List;
import java.util.LinkedList;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.*;

public abstract class HtmlNode implements Node, Cloneable {

    public abstract String getNodeName();
    public abstract String getNodeValue();
    public abstract void setNodeValue(String nodeValue) throws DOMException;
    public abstract short getNodeType();
    public abstract Node getParentNode();
    public abstract NodeList getChildNodes();
    public abstract Node getFirstChild();
    public abstract Node getLastChild();
    public abstract Node getPreviousSibling();
    public abstract Node getNextSibling();
    public abstract NamedNodeMap getAttributes();
    public abstract Document getOwnerDocument();
    public abstract Node insertBefore(Node newChild, Node refChild) throws DOMException;
    public abstract Node replaceChild(Node newChild, Node oldChild) throws DOMException;
    public abstract Node removeChild(Node oldChild) throws DOMException;
    public abstract Node appendChild(Node newChild) throws DOMException;
    public abstract boolean hasChildNodes();
    public abstract Node cloneNode(boolean deep);
    public abstract void normalize();
    public abstract boolean isSupported(String feature, String version);
    public abstract String getNamespaceURI();
    public abstract String getPrefix();
    public abstract void setPrefix(String prefix) throws DOMException;
    public abstract String getLocalName();
    public abstract boolean hasAttributes();
    public abstract String getBaseURI();
    public abstract short compareDocumentPosition(Node other) throws DOMException;
    public abstract String getTextContent() throws DOMException;
    public abstract void setTextContent(String textContent) throws DOMException;
    public abstract boolean isSameNode(Node other);
    public abstract String lookupPrefix(String namespaceURI);
    public abstract boolean isDefaultNamespace(String namespaceURI);
    public abstract String lookupNamespaceURI(String prefix);
    public abstract boolean isEqualNode(Node arg);
    public abstract Object getFeature(String feature, String version);
    public abstract Object setUserData(String key, Object data, UserDataHandler handler);
    public abstract Object getUserData(String key);
    

    @Override
    public HtmlNode clone() {
        HtmlNode thisClone = doClone(null); // splits for orphan

        // Queue up nodes that need their children cloned (BFS).
        final LinkedList<HtmlNode> nodesToProcess = new LinkedList<>();
        nodesToProcess.add(thisClone);

        while (!nodesToProcess.isEmpty()) {
            HtmlNode currParent = nodesToProcess.remove();

            final int size = currParent.childNodeSize();
            for (int i = 0; i < size; i++) {
                final List<HtmlNode> childNodes = currParent.ensureChildNodes();
                HtmlNode childClone = childNodes.get(i).doClone(currParent);
                childNodes.set(i, childClone);
                nodesToProcess.add(childClone);
            }
        }
        return thisClone;
    }

    protected HtmlNode doClone(@Nullable HtmlNode parent) {
        assert parent == null || parent instanceof Element;
        HtmlNode clone;

        try {
            clone = (HtmlNode) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        clone.parentNode = (Element) parent; // can be null, to create an orphan split
        clone.siblingIndex = parent == null ? 0 : siblingIndex();
        // if not keeping the parent, shallowClone the ownerDocument to preserve its settings
        if (parent == null && !(this instanceof Document)) {
            Document doc = ownerDocument();
            if (doc != null) {
                Document docClone = doc.shallowClone();
                clone.parentNode = docClone;
                docClone.ensureChildNodes().add(clone);
            }
        }
        return clone;
    }
}
