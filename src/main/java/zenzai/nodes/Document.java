package zenzai.nodes;

import java.nio.charset.Charset;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.DOMException;

import org.w3c.dom.NamedNodeMap;
import zenzai.helper.DataUtil;
import zenzai.helper.Validate;
import zenzai.parser.ParseSettings;
import zenzai.parser.Parser;
import zenzai.parser.Tag;

import static zenzai.parser.Parser.NamespaceHtml;

public abstract class Document extends Element implements org.w3c.dom.Document {
    private OutputSettings outputSettings = new OutputSettings();
    private Parser parser; // the parser used to parse this document
    private QuirksMode quirksMode = QuirksMode.noQuirks;
    private final String location;

    /**
     Create a new, empty Document, in the specified namespace.
     @param namespace the namespace of this Document's root node.
     @param baseUri base URI of document
     @see org.jsoup.Jsoup#parse
     @see #createShell
     */
    public Document(String namespace, String baseUri) {
        this(namespace, baseUri, Parser.htmlParser()); // default HTML parser, but overridable
    }

    /**
     Create a new, empty Document, in the HTML namespace.
     @param baseUri base URI of document
     @see org.jsoup.Jsoup#parse
     @see #Document(String namespace, String baseUri)
     */
    public Document(String baseUri) {
        this(NamespaceHtml, baseUri);
    }

    private Document(String namespace, String baseUri, Parser parser) {
        super(new Tag("#root", namespace), baseUri);
        this.location = baseUri;
        this.parser = parser;
    }

    @Override
    public Document clone() {
        Document clone = (Document) super.clone();
        if (attributes != null) clone.attributes = attributes.clone();
        clone.outputSettings = this.outputSettings.clone();
        // parser is pointer copy
        return clone;
    }

    @Override
    public Document shallowClone() {
        Document clone = new Document(this.tag().namespace(), baseUri(), parser); // preserves parser pointer
        if (attributes != null) clone.attributes = attributes.clone();
        clone.outputSettings = this.outputSettings.clone();
        return clone;
    }

    // org.w3c.dom.Node
    @Override
    public String getNodeName() {
        return "#document";
    }

    // org.w3d.dom.Node
    @Override
    public String getNodeValue() throws DOMException {
        return null;
    }

    // org.w3c.dom.Node
    @Override
    public void setNodeValue(String value) throws DOMException {
        // no-op
    }

    // org.w3c.dom.Node
    @Override
    public short getNodeType() {
        return Node.DOCUMENT_NODE;
    }

    // org.w3c.dom.Node
    @Override
    public org.w3c.dom.Node getParentNode() {
        return null;
    }

    // org.w3c.dom.Node
    @Override
    public NamedNodeMap getAttributes() { return null; }

    // org.w3c.dom.Node
    @Override
    public Node insertBefore(org.w3c.dom.Node newChild, org.w3c.dom.Node refChild) throws DOMException {
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported for this type of node.");
    }

    // org.w3c.dom.Node
    @Override
    public String getTextContent() throws DOMException {
        return null;
    }

    // org.w3c.dom.Node
    @Override
    public void setTextContent(String textContent) throws DOMException {
        // no-op
    }

    // org.w3c.dom.Document
    @Override
    public org.w3c.dom.DocumentType getDoctype() { return documentType(); }
    // org.w3c.dom.Document
    @Override
    public org.w3c.dom.DOMImplementation getImplementation() { return null; }
    public org.w3c.dom.Element getDocumentElement() { return getElementsByTag("html").getFirst(); }

    // org.w3c.dom.Document
    // zenzai.nodes.Document
    @Override
    public Element createElement(String tagName) throws DOMException {
        Element element =  new Element(
                parser.tagSet().valueOf(tagName, parser.defaultNamespace(), ParseSettings.preserveCase),
                searchUpForAttribute(this, BaseUriKey)
        );
        element.setOwnerDocument(this);
        return element;
    }
    // org.w3c.dom.Document
    @Override
    public org.w3c.dom.DocumentFragment createDocumentFragment() {
        // TODO: create DocumentFragment class and implement this method
        return null;
    }
    // org.w3c.dom.Document
    @Override
    public org.w3c.dom.Text createTextNode(String data) {
        TextNode textNode = new TextNode(data);
        textNode.setOwnerDocument(this);
        return textNode;
    }
    // org.w3c.dom.Document
    @Override
    public org.w3c.dom.Comment createComment(String data) {
        Comment comment = new Comment(data);
        comment.setOwnerDocument(this);
        return comment;
    }
    // org.w3c.dom.Document
    @Override
    public org.w3c.dom.CDATASection createCDATASection(String data) {
        CDataNode cDataNode = new CDataNode(data);
        cDataNode.setOwnerDocument(this);
        return cDataNode;
    }
    public org.w3c.dom.ProcessingInstruction createProcessingInstruction(String target, String data) throws DOMException {
        // TODO: create ProcessingInstruction class and implement this method
        throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Not supported for this type of node.");
    }
    public abstract org.w3c.dom.Attr createAttribute(String name);
    public abstract org.w3c.dom.EntityReference createEntityReference(String name) throws DOMException;
    public abstract org.w3c.dom.NodeList getElementsByTagName(String tagName);
    public abstract org.w3c.dom.Node importNode(Node importedNode, boolean deep) throws DOMException;
    public abstract org.w3c.dom.Element createElementNS(String namespaceURI, String qualifiedName) throws DOMException;
    public abstract org.w3c.dom.Attr createAttributeNS(String namespaceURI, String qualifiedName) throws DOMException;
    public abstract org.w3c.dom.NodeList getElementsByTagNameNS(String namespaceURI, String localName);
    public abstract org.w3c.dom.Element getElementById(String id);
    public abstract String getInputEncoding();
    public abstract String getXmlEncoding();
    public abstract boolean getXmlStandalone();
    public abstract void setXmlStandalone(boolean xmlStandalone);
    public abstract String getXmlVersion();
    public abstract void setXmlVersion(String xmlVersion) throws DOMException;
    public abstract boolean getStrictErrorChecking();
    public abstract void setStrictErrorChecking(boolean strictErrorChecking);
    public abstract String getDocumentURI();
    public abstract void setDocumentURI(String documentURI);
    public abstract org.w3c.dom.Node adoptNode(Node node) throws DOMException;
    public abstract org.w3c.dom.DOMConfiguration getDomConfig();
    public abstract void normalizeDocument();
    public abstract org.w3c.dom.Node renameNode(Node n, String namespaceURI, String qualifiedName) throws DOMException;

    @Override
    public String outerHtml() {
        return super.html(); // no outer wrapper tag
    }

    /**
     Set the text of the {@code body} of this document. Any existing nodes within the body will be cleared.
     @param text un-encoded text
     @return this document
     */
    @Override
    public Element text(String text) {
        body().text(text); // overridden to not nuke doc structure
        return this;
    }

    @Override
    public String nodeName() {
        return "#document";
    }

    /**
     Create a valid, empty shell of an HTML document, suitable for adding more elements to.
     @param baseUri baseUri of document
     @return document with html, head, and body elements.
     */
    public static Document createShell(String baseUri) {
        Validate.notNull(baseUri);

        Document doc = new Document(baseUri);
        Element html = doc.appendElement("html");
        html.appendElement("head");
        html.appendElement("body");

        return doc;
    }

    /**
     * Get the URL this Document was parsed from. If the starting URL is a redirect,
     * this will return the final URL from which the document was served from.
     * <p>Will return an empty string if the location is unknown (e.g. if parsed from a String).
     * @return location
     */
    public String location() {
        return location;
    }

    /**
     * Returns this Document's doctype.
     * @return document type, or null if not set
     */
    public @Nullable DocumentType documentType() {
        for (Node node : childNodes) {
            if (node instanceof DocumentType)
                return (DocumentType) node;
            else if (!(node instanceof LeafNode)) // scans forward across comments, text, processing instructions etc
                break;
        }
        return null;
    }

    /**
     Get this document's {@code head} element.
     <p>
     As a side effect, if this Document does not already have an HTML structure, it will be created. If you do not want
     that, use {@code #selectFirst("head")} instead.

     @return {@code head} element.
     */
    public Element head() {
        final Element html = htmlEl();
        Element el = html.firstElementChild();
        while (el != null) {
            if (el.nameIs("head"))
                return el;
            el = el.nextElementSibling();
        }
        return html.prependElement("head");
    }

    /**
     Get this document's {@code <body>} or {@code <frameset>} element.
     <p>
     As a <b>side-effect</b>, if this Document does not already have an HTML structure, it will be created with a {@code
    <body>} element. If you do not want that, use {@code #selectFirst("body")} instead.

     @return {@code body} element for documents with a {@code <body>}, a new {@code <body>} element if the document
     had no contents, or the outermost {@code <frameset> element} for frameset documents.
     */
    public Element body() {
        final Element html = htmlEl();
        Element el = html.firstElementChild();
        while (el != null) {
            if (el.nameIs("body") || el.nameIs("frameset"))
                return el;
            el = el.nextElementSibling();
        }
        return html.appendElement("body");
    }

    /**
     * Get the parser that was used to parse this document.
     * @return the parser
     */
    public Parser parser() {
        return parser;
    }

    /**
     * Set the parser used to create this document. This parser is then used when further parsing within this document
     * is required.
     * @param parser the configured parser to use when further parsing is required for this document.
     * @return this document, for chaining.
     */
    public Document parser(Parser parser) {
        this.parser = parser;
        return this;
    }

    public enum QuirksMode {
        noQuirks, quirks, limitedQuirks
    }

    public QuirksMode quirksMode() {
        return quirksMode;
    }

    public Document quirksMode(QuirksMode quirksMode) {
        this.quirksMode = quirksMode;
        return this;
    }

    /**
     Get the output character set of this Document. This method is equivalent to {@link OutputSettings#charset()}.

     @return the current Charset
     @see OutputSettings#charset()
     */
    public Charset charset() {
        return outputSettings.charset();
    }

    /**
     Find the root HTML element, or create it if it doesn't exist.
     @return the root HTML element.
     */
    private Element htmlEl() {
        Element el = firstElementChild();
        while (el != null) {
            if (el.nameIs("html"))
                return el;
            el = el.nextElementSibling();
        }
        return appendElement("html");
    }

    /**
     * Get the document's current output settings.
     * @return the document's current output settings.
     */
    public OutputSettings outputSettings() {
        return outputSettings;
    }

    /**
     * Set the document's output settings.
     * @param outputSettings new output settings.
     * @return this document, for chaining.
     */
    public Document outputSettings(OutputSettings outputSettings) {
        Validate.notNull(outputSettings);
        this.outputSettings = outputSettings;
        return this;
    }

    /**
     * A Document's output settings control the form of the text() and html() methods.
     */
    public static class OutputSettings implements Cloneable {
        /**
         * The output serialization syntax.
         */
        public enum Syntax {html, xml}
        private Entities.EscapeMode escapeMode = Entities.EscapeMode.base;
        private Charset charset = DataUtil.UTF_8;
        private boolean prettyPrint = true;
        private boolean outline = false;
        private int indentAmount = 1;
        private int maxPaddingWidth = 30;
        private Syntax syntax = Syntax.html;

        /**
         Create a new OutputSettings object, with the default settings (UTF-8, HTML, EscapeMode.base, pretty-printing,
         indent amount of 1).
         */
        public OutputSettings() {
        }

        @Override
        public OutputSettings clone() {
            OutputSettings clone;
            try {
                clone = (OutputSettings) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
            clone.charset(charset.name()); // new charset, coreCharset, and charset encoder
            clone.escapeMode = Entities.EscapeMode.valueOf(escapeMode.name());
            // indentAmount, maxPaddingWidth, and prettyPrint are primitives so object.clone() will handle
            return clone;
        }

        /**
         Get the document's current entity escape mode:
         <ul>
         <li><code>xhtml</code>, the minimal named entities in XHTML / XML</li>
         <li><code>base</code>, which provides a limited set of named HTML
         entities and escapes other characters as numbered entities for maximum compatibility</li>
         <li><code>extended</code>,
         which uses the complete set of HTML named entities.</li>
         </ul>
         <p>The default escape mode is <code>base</code>.
         @return the document's current escape mode
         */
        public Entities.EscapeMode escapeMode() {
            return escapeMode;
        }

        /**
         * Set the document's escape mode, which determines how characters are escaped when the output character set
         * does not support a given character:- using either a named or a numbered escape.
         * @param escapeMode the new escape mode to use
         * @return the document's output settings, for chaining
         */
        public OutputSettings escapeMode(Entities.EscapeMode escapeMode) {
            this.escapeMode = escapeMode;
            return this;
        }

        /**
         * Get the document's current output charset, which is used to control which characters are escaped when
         * generating HTML (via the <code>html()</code> methods), and which are kept intact.
         * <p>
         * Where possible (when parsing from a URL or File), the document's output charset is automatically set to the
         * input charset. Otherwise, it defaults to UTF-8.
         * @return the document's current charset.
         */
        public Charset charset() {
            return charset;
        }

        /**
         * Update the document's output charset.
         * @param charset the new charset to use.
         * @return the document's output settings, for chaining
         */
        public OutputSettings charset(Charset charset) {
            this.charset = charset;
            return this;
        }

        /**
         * Update the document's output charset.
         * @param charset the new charset (by name) to use.
         * @return the document's output settings, for chaining
         */
        public OutputSettings charset(String charset) {
            charset(Charset.forName(charset));
            return this;
        }

        /**
         * Get the document's current output syntax.
         * @return current syntax
         */
        public Syntax syntax() {
            return syntax;
        }

        /**
         * Set the document's output syntax. Either {@code html}, with empty tags and boolean attributes (etc), or
         * {@code xml}, with self-closing tags.
         * <p>When set to {@link Document.OutputSettings.Syntax#xml xml}, the {@link #escapeMode() escapeMode} is
         * automatically set to {@link Entities.EscapeMode#xhtml}, but may be subsequently changed if desired.</p>
         * @param syntax serialization syntax
         * @return the document's output settings, for chaining
         */
        public OutputSettings syntax(Syntax syntax) {
            this.syntax = syntax;
            if (syntax == Syntax.xml)
                this.escapeMode(Entities.EscapeMode.xhtml);
            return this;
        }

        /**
         * Get if pretty printing is enabled. Default is true. If disabled, the HTML output methods will not re-format
         * the output, and the output will generally look like the input.
         * @return if pretty printing is enabled.
         */
        public boolean prettyPrint() {
            return prettyPrint;
        }

        /**
         * Enable or disable pretty printing.
         * @param pretty new pretty print setting
         * @return this, for chaining
         */
        public OutputSettings prettyPrint(boolean pretty) {
            prettyPrint = pretty;
            return this;
        }

        /**
         * Get if outline mode is enabled. Default is false. If enabled, the HTML output methods will consider
         * all tags as block.
         * @return if outline mode is enabled.
         */
        public boolean outline() {
            return outline;
        }

        /**
         * Enable or disable HTML outline mode.
         * @param outlineMode new outline setting
         * @return this, for chaining
         */
        public OutputSettings outline(boolean outlineMode) {
            outline = outlineMode;
            return this;
        }

        /**
         * Get the current tag indent amount, used when pretty printing.
         * @return the current indent amount
         */
        public int indentAmount() {
            return indentAmount;
        }

        /**
         * Set the indent amount for pretty printing
         * @param indentAmount number of spaces to use for indenting each level. Must be {@literal >=} 0.
         * @return this, for chaining
         */
        public OutputSettings indentAmount(int indentAmount) {
            Validate.isTrue(indentAmount >= 0);
            this.indentAmount = indentAmount;
            return this;
        }

        /**
         * Get the current max padding amount, used when pretty printing
         * so very deeply nested nodes don't get insane padding amounts.
         * @return the current indent amount
         */
        public int maxPaddingWidth() {
            return maxPaddingWidth;
        }

        /**
         * Set the max padding amount for pretty printing so very deeply nested nodes don't get insane padding amounts.
         * @param maxPaddingWidth number of spaces to use for indenting each level of nested nodes. Must be {@literal >=} -1.
         *        Default is 30 and -1 means unlimited.
         * @return this, for chaining
         */
        public OutputSettings maxPaddingWidth(int maxPaddingWidth) {
            Validate.isTrue(maxPaddingWidth >= -1);
            this.maxPaddingWidth = maxPaddingWidth;
            return this;
        }
    }
}
