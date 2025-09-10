package zenzai.helper;

import org.w3c.dom.DOMException;

import zenzai.nodes.Node;

public final class W3CValidation {
    private W3CValidation() {}
    /*
    insertBefore
        DOMException - HIERARCHY_REQUEST_ERR: Raised if this node is of a type that does not allow children of the
        type of the newChild node, or if the node to insert is one of this node's ancestors or this node itself, or
        if this node is of type Document and the DOM application attempts to insert a second DocumentType or Element node.
        WRONG_DOCUMENT_ERR: Raised if newChild was created from a different document than the one that created this node.
        NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly or if the parent of the node being inserted is readonly.
        NOT_FOUND_ERR: Raised if refChild is not a child of this node.
         */

    /*
    replaceChild
    DOMException - HIERARCHY_REQUEST_ERR: Raised if this node is of a type that does not allow children of the type of
    the newChild node, or if the node to put in is one of this node's ancestors or this node itself, or if this node is
    of type Document and the result of the replacement operation would add a second DocumentType or Element on the Document node.
WRONG_DOCUMENT_ERR: Raised if newChild was created from a different document than the one that created this node.
NO_MODIFICATION_ALLOWED_ERR: Raised if this node or the parent of the new node is readonly.
NOT_FOUND_ERR: Raised if oldChild is not a child of this node.
NOT_SUPPORTED_ERR: if this node is of type Document, this exception might be raised if the DOM implementation doesn't support the replacement of the DocumentType child or Element child.
     */

    /*
    removeChild
    DOMException - NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly.
NOT_FOUND_ERR: Raised if oldChild is not a child of this node.
NOT_SUPPORTED_ERR: if this node is of type Document, this exception might be raised if the DOM implementation doesn't support the removal of the DocumentType child or the Element child.
     */

    /*
    appendChild
    DOMException - HIERARCHY_REQUEST_ERR: Raised if this node is of a type that does not allow children of the type of the newChild node, or if the node to append is one of this node's ancestors or this node itself, or if this node is of type Document and the DOM application attempts to append a second DocumentType or Element node.
WRONG_DOCUMENT_ERR: Raised if newChild was created from a different document than the one that created this node.
NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly or if the previous parent of the node being inserted is readonly.
NOT_SUPPORTED_ERR: if the newChild node is a child of the Document node, this exception might be raised if the DOM implementation doesn't support the removal of the DocumentType child or Element child.
     */

    public static void hierarchyRequest(Node base, Node other) {
        // TODO: elaborate this method
        if (other == base || other == base.getParentNode() || other instanceof org.w3c.dom.Document) {
            throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "Cannot perform this operation because of hierarchy request error.");
        }
    }

    public static void wrongDocument(Node base, Node other) {
        if (base.ownerDocument() != other.ownerDocument()) {
            throw new DOMException(DOMException.WRONG_DOCUMENT_ERR, "Cannot perform this operation because of wrong document error.");
        }
    }

    public static void modificationAllowed(Node base, Node other) {
        if (base instanceof org.w3c.dom.DocumentType || base instanceof org.w3c.dom.Entity ||
                other.parentNode() instanceof org.w3c.dom.DocumentType || other.parentNode() instanceof org.w3c.dom.Entity) {
            throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "Cannot perform this operation because of no modification allowed error.");
        }
    }

    public static void nodeInChildren(Node base, Node other) {
        if (other.parentNode() != base) {
            throw new DOMException(DOMException.NOT_FOUND_ERR, "Cannot perform this operation because of not found error.");
        }
    }

    public static void operationSupported(Node base) {
        if (base instanceof org.w3c.dom.Document) {
            throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Cannot perform this operation because of not supported error.");
        }
    }
}
