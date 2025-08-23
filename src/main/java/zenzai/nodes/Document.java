package zenzai.nodes;

import java.nio.charset.Charset;

import org.w3c.dom.DOMException;

import zenzai.helper.DataUtil;
import zenzai.helper.Validate;
import zenzai.parser.Parser;
import zenzai.parser.Tag;

import static zenzai.parser.Parser.NamespaceHtml;

public abstract class Document extends Element implements org.w3c.dom.Document {
    private OutputSettings outputSettings = new OutputSettings();
    private Parser parser; // the parser used to parse this document
    private QuirksMode quirksMode = QuirksMode.noQuirks;
    private final String location;

    public abstract org.w3c.dom.DocumentType getDoctype();
    public abstract org.w3c.dom.DOMImplementation getImplementation();
    public abstract org.w3c.dom.Element getDocumentElement();
    public abstract org.w3c.dom.Element createElement(String tagName) throws DOMException;
    public abstract org.w3c.dom.DocumentFragment createDocumentFragment();
    public abstract org.w3c.dom.Text createTextNode(String data);
    public abstract org.w3c.dom.Comment createComment(String data);
    public abstract org.w3c.dom.CDATASection createCDATASection(String data);
    public abstract org.w3c.dom.ProcessingInstruction createProcessingInstruction(String target, String data) throws DOMException;
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
    public String getNodeName() {
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

    @Override
    public Document shallowClone() {
        Document clone = new Document(this.tag().namespace(), baseUri(), parser); // preserves parser pointer
        if (attributes != null) clone.attributes = attributes.clone();
        clone.outputSettings = this.outputSettings.clone();
        return clone;
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
    }
}
