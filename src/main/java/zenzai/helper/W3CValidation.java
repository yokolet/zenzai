package zenzai.helper;

import org.w3c.dom.DOMException;

import zenzai.nodes.Node;

public final class W3CValidation {
    private W3CValidation() {}

    /*
    insertBefore
        Raised if this node is of a type that does not allow children of the type of the newChild node, or if the node
        to insert is one of this node's ancestors or this node itself, or if this node is of type Document and the DOM
        application attempts to insert a second DocumentType or Element node.
    replaceChild
        Raised if this node is of a type that does not allow children of the type of the newChild node, or if the node
        to put in is one of this node's ancestors or this node itself, or if this node is of type Document and the
        result of the replacement operation would add a second DocumentType or Element on the Document node.
    appendChild
        Raised if this node is of a type that does not allow children of the type of the newChild node, or if the node
        to append is one of this node's ancestors or this node itself, or if this node is of type Document and the DOM
        application attempts to append a second DocumentType or Element node.
     */
    public static void hierarchyRequest(Node base, Node other) {
        // TODO: elaborate this method
        if (other == base || other == base.getParentNode() || other instanceof org.w3c.dom.Document) {
            throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "Cannot perform this operation because of hierarchy request error.");
        }
    }

    /*
    NamedNodeMap.setNamedItem
        Raised if an attempt is made to add a node doesn't belong in this NamedNodeMap. Examples would include trying
        to insert something other than an Attr node into an Element's map of attributes, or a non-Entity node into the
        DocumentType's map of Entities.
     */
    public static void attrHierarchyRequest(Node base, Node other) {
        // TODO: elaborate this method
        if (base instanceof org.w3c.dom.Element && !(other instanceof org.w3c.dom.Attr)) {
            throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "Cannot perform this operation because of hierarchy request error.");
        }
        if (base instanceof org.w3c.dom.DocumentType && !(other instanceof org.w3c.dom.Entity)) {
            throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "Cannot perform this operation because of hierarchy request error.");
        }
    }

    /*
    insertBefore
        Raised if newChild was created from a different document than the one that created this node.
    replaceChild
        Raised if newChild was created from a different document than the one that created this node.
    appendChild
        Raised if newChild was created from a different document than the one that created this node.
     */
    public static void wrongDocument(Node base, Node other) {
        if (base.ownerDocument() != other.ownerDocument()) {
            throw new DOMException(DOMException.WRONG_DOCUMENT_ERR, "Cannot perform this operation because of wrong document error.");
        }
    }

    /*
    insertBefore
        Raised if this node is readonly or if the parent of the node being inserted is readonly.
    replaceChild
        Raised if this node or the parent of the new node is readonly.
    removeChild
        Raised if this node is readonly.
    appendChild
        Raised if this node is readonly or if the previous parent of the node being inserted is readonly.
    Element.setAttribute
        Raised if this node is readonly.
    Element.removeAttribute
        Raised if this node is readonly.
    CharacterData.appendData
        Raised if this node is readonly.
    Text.splitText
        Raised if this node is readonly.
    NamedNodeMap.setNamedItem
        Raised if this map is readonly.
     */
    public static void modificationAllowed(Node... nodes) {
        if (nodes.length == 0) return;
        if (nodes.length >= 1 && (nodes[0] instanceof org.w3c.dom.DocumentType || nodes[0] instanceof org.w3c.dom.Entity)) {
            throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "Cannot perform this operation because of no modification allowed error.");
        }
        if (nodes.length >= 2 && (nodes[1].parentNode() instanceof org.w3c.dom.DocumentType || nodes[1].parentNode() instanceof org.w3c.dom.Entity)) {
            throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "Cannot perform this operation because of no modification allowed error.");
        }
    }

    /*
    insertBefore
        Raised if refChild is not a child of this node.
    replaceChild
        Raised if oldChild is not a child of this node.
    removeChild
        Raised if oldChild is not a child of this node.
     */
    public static void nodeInChildren(Node base, Node other) {
        if (other.parentNode() != base) {
            throw new DOMException(DOMException.NOT_FOUND_ERR, "Cannot perform this operation because of not found error.");
        }
    }

    /*
    insertBefore
        if this node is of type Document, this exception might be raised if the DOM implementation doesn't support
        the insertion of a DocumentType or Element node.
    replaceChild
        if this node is of type Document, this exception might be raised if the DOM implementation doesn't support
        the replacement of the DocumentType child or Element child.
    removeChild
        if this node is of type Document, this exception might be raised if the DOM implementation doesn't support
        the removal of the DocumentType child or the Element child.
    appendChild
        if the newChild node is a child of the Document node, this exception might be raised if the DOM implementation
        doesn't support the removal of the DocumentType child or Element child.
     */
    public static void operationSupported(Node base) {
        if (base instanceof org.w3c.dom.Document) {
            throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Cannot perform this operation because of not supported error.");
        }
    }

    /*
    CharacterData.substringData
        Raised if the specified offset is negative or greater than the number of 16-bit units in data, or if the
        specified count is negative.
    CharacterData.insertData
        Raised if the specified offset is negative or greater than the number of 16-bit units in data.
    Text.splitText
        Raised if the specified offset is negative or greater than the number of 16-bit units in data.
     */
    public static void indexSizeWithinLength(Node base, int... args) {
        if (base instanceof zenzai.nodes.TextNode || base instanceof org.w3c.dom.CDATASection || base instanceof org.w3c.dom.Comment) {
            if (args.length > 0 && (args[0] < 0 || args[0] > base.nodeValue().length() - 1)) {
                throw new DOMException(DOMException.INDEX_SIZE_ERR, "Cannot perform this operation because of index size error.");
            }
            if (args.length > 1 && args[1] < 0) {
                throw new DOMException(DOMException.INDEX_SIZE_ERR, "Cannot perform this operation because of index size error.");
            }
        }
    }

    /*
    NamedNodeMap.removeNamedItem
        Raised if there is no node named name in this map.
     */
    public static void attrInMap(Node base, String name) {
        if (base instanceof org.w3c.dom.Element && base.attributes() != null && base.attributes().hasKey(name)) {
            throw new DOMException(DOMException.NOT_FOUND_ERR, "Cannot perform this operation because of not found error.");
        }
    }
}
