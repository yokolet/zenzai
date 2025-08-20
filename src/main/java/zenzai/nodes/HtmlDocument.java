package zenzai.nodes;

import java.nio.charset.Charset;

import org.w3c.dom.*;

import zenzai.helper.DataUtil;
import zenzai.helper.Validate;
import zenzai.parser.HtmlParser;
import zenzai.parser.Tag;

import static zenzai.parser.HtmlParser.NamespaceHtml;

public abstract class HtmlDocument extends HtmlElement implements Document {
    private OutputSettings outputSettings = new OutputSettings();
    private HtmlParser parser; // the parser used to parse this document
    private QuirksMode quirksMode = QuirksMode.noQuirks;
    private final String location;

    public abstract DocumentType getDoctype();
    public abstract DOMImplementation getImplementation();
    public abstract Element getDocumentElement();
    public abstract Element createElement(String tagName) throws DOMException;
    public abstract DocumentFragment createDocumentFragment();
    public abstract Text createTextNode(String data);
    public abstract Comment createComment(String data);
    public abstract CDATASection createCDATASection(String data);
    public abstract ProcessingInstruction createProcessingInstruction(String target, String data) throws DOMException;
    public abstract Attr createAttribute(String name);
    public abstract EntityReference createEntityReference(String name) throws DOMException;
    public abstract NodeList getElementsByTagName(String tagName);
    public abstract Node importNode(Node importedNode, boolean deep) throws DOMException;
    public abstract Element createElementNS(String namespaceURI, String qualifiedName) throws DOMException;
    public abstract Attr createAttributeNS(String namespaceURI, String qualifiedName) throws DOMException;
    public abstract NodeList getElementsByTagNameNS(String namespaceURI, String localName);
    public abstract Element getElementById(String id);
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
    public abstract Node adoptNode(Node node) throws DOMException;
    public abstract DOMConfiguration getDomConfig();
    public abstract void normalizeDocument();
    public abstract Node renameNode(Node n, String namespaceURI, String qualifiedName) throws DOMException;

    /**
     Create a new, empty Document, in the specified namespace.
     @param namespace the namespace of this Document's root node.
     @param baseUri base URI of document
     @see org.jsoup.Jsoup#parse
     @see #createShell
     */
    public HtmlDocument(String namespace, String baseUri) {
        this(namespace, baseUri, HtmlParser.htmlParser()); // default HTML parser, but overridable
    }

    /**
     Create a new, empty Document, in the HTML namespace.
     @param baseUri base URI of document
     @see org.jsoup.Jsoup#parse
     @see #HtmlDocument(String namespace, String baseUri)
     */
    public HtmlDocument(String baseUri) {
        this(NamespaceHtml, baseUri);
    }

    private HtmlDocument(String namespace, String baseUri, HtmlParser parser) {
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
    public static HtmlDocument createShell(String baseUri) {
        Validate.notNull(baseUri);

        HtmlDocument doc = new HtmlDocument(baseUri);
        HtmlElement html = doc.appendElement("html");
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
    public HtmlElement body() {
        final HtmlElement html = htmlEl();
        HtmlElement el = html.firstElementChild();
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
    public HtmlParser parser() {
        return parser;
    }

    /**
     * Set the parser used to create this document. This parser is then used when further parsing within this document
     * is required.
     * @param parser the configured parser to use when further parsing is required for this document.
     * @return this document, for chaining.
     */
    public HtmlDocument parser(HtmlParser parser) {
        this.parser = parser;
        return this;
    }

    public enum QuirksMode {
        noQuirks, quirks, limitedQuirks
    }

    public QuirksMode quirksMode() {
        return quirksMode;
    }

    public HtmlDocument quirksMode(QuirksMode quirksMode) {
        this.quirksMode = quirksMode;
        return this;
    }

    /**
     Find the root HTML element, or create it if it doesn't exist.
     @return the root HTML element.
     */
    private HtmlElement htmlEl() {
        HtmlElement el = firstElementChild();
        while (el != null) {
            if (el.nameIs("html"))
                return el;
            el = el.nextElementSibling();
        }
        return appendElement("html");
    }

    @Override
    public HtmlDocument shallowClone() {
        HtmlDocument clone = new HtmlDocument(this.tag().namespace(), baseUri(), parser); // preserves parser pointer
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
        private HtmlEntities.EscapeMode escapeMode = HtmlEntities.EscapeMode.base;
        private Charset charset = DataUtil.UTF_8;

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
            clone.escapeMode = HtmlEntities.EscapeMode.valueOf(escapeMode.name());
            // indentAmount, maxPaddingWidth, and prettyPrint are primitives so object.clone() will handle
            return clone;
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
    }

}
