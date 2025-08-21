package zenzai.parser;

import org.jspecify.annotations.Nullable;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import zenzai.helper.Validate;
import zenzai.internal.SharedConstants;
import zenzai.nodes.Attributes;
import zenzai.nodes.HtmlDocument;
import zenzai.nodes.Node;
import zenzai.nodes.HtmlElement;
import zenzai.nodes.HtmlRange;
import zenzai.select.NodeVisitor;

import static zenzai.parser.Parser.NamespaceHtml;

/**
 * @author Jonathan Hedley
 */
abstract class TreeBuilder {
    protected Parser parser;
    CharacterReader reader;
    Tokeniser tokeniser;
    HtmlDocument doc; // current doc we are building into
    ArrayList<HtmlElement> stack; // the stack of open elements
    String baseUri; // current base uri, for creating new elements
    Token currentToken; // currentToken is used for error and source position tracking. Null at start of fragment parse
    ParseSettings settings;
    TagSet tagSet; // the tags we're using in this parse
    @Nullable NodeVisitor nodeListener; // optional listener for node add / removes

    private Token.StartTag start; // start tag to process
    private final Token.EndTag end  = new Token.EndTag(this);
    abstract ParseSettings defaultSettings();

    boolean trackSourceRange;  // optionally tracks the source range of nodes and attributes

    void initialiseParse(Reader input, String baseUri, Parser parser) {
        Validate.notNullParam(input, "input");
        Validate.notNullParam(baseUri, "baseUri");
        Validate.notNull(parser);

        doc = new HtmlDocument(parser.defaultNamespace(), baseUri);
        doc.parser(parser);
        this.parser = parser;
        settings = parser.settings();
        reader = new CharacterReader(input);
        trackSourceRange = parser.isTrackPosition();
        reader.trackNewlines(parser.isTrackErrors() || trackSourceRange); // when tracking errors or source ranges, enable newline tracking for better legibility
        if (parser.isTrackErrors()) parser.getErrors().clear();
        tokeniser = new Tokeniser(this);
        stack = new ArrayList<>(32);
        tagSet = parser.tagSet();
        start = new Token.StartTag(this);
        currentToken = start; // init current token to the virtual start token.
        this.baseUri = baseUri;
        onNodeInserted(doc);
    }

    void completeParse() {
        // tidy up - as the Parser and Treebuilder are retained in document for settings / fragments
        if (reader == null) return;
        reader.close();
        reader = null;
        tokeniser = null;
        stack = null;
    }

    HtmlDocument parse(Reader input, String baseUri, Parser parser) {
        initialiseParse(input, baseUri, parser);
        runParser();
        return doc;
    }

    List<zenzai.nodes.Node> parseFragment(Reader inputFragment, @Nullable HtmlElement context, String baseUri, Parser parser) {
        initialiseParse(inputFragment, baseUri, parser);
        initialiseParseFragment(context);
        runParser();
        return completeParseFragment();
    }

    void initialiseParseFragment(@Nullable HtmlElement context) {
        // in Html, sets up context; no-op in XML
    }

    abstract List<zenzai.nodes.Node> completeParseFragment();

    /** Set the node listener, which will then get callbacks for node insert and removals. */
    void nodeListener(NodeVisitor nodeListener) {
        this.nodeListener = nodeListener;
    }

    /**
     Create a new copy of this TreeBuilder
     @return copy, ready for a new parse
     */
    abstract TreeBuilder newInstance();

    void runParser() {
        do {} while (stepParser()); // run until stepParser sees EOF
        completeParse();
    }

    boolean stepParser() {
        // if we have reached the end already, step by popping off the stack, to hit nodeRemoved callbacks:
        if (currentToken.type == Token.TokenType.EOF) {
            if (stack == null) {
                return false;
            } if (stack.isEmpty()) {
                onNodeClosed(doc); // the root doc is not on the stack, so let this final step close it
                stack = null;
                return true;
            }
            pop();
            return true;
        }
        final Token token = tokeniser.read();
        currentToken = token;
        process(token);
        token.reset();
        return true;
    }

    abstract boolean process(Token token);

    boolean processStartTag(String name) {
        // these are "virtual" start tags (auto-created by the treebuilder), so not tracking the start position
        final Token.StartTag start = this.start;
        if (currentToken == start) { // don't recycle an in-use token
            return process(new Token.StartTag(this).name(name));
        }
        return process(start.reset().name(name));
    }

    boolean processStartTag(String name, Attributes attrs) {
        final Token.StartTag start = this.start;
        if (currentToken == start) { // don't recycle an in-use token
            return process(new Token.StartTag(this).nameAttr(name, attrs));
        }
        start.reset();
        start.nameAttr(name, attrs);
        return process(start);
    }

    boolean processEndTag(String name) {
        if (currentToken == end) { // don't recycle an in-use token
            return process(new Token.EndTag(this).name(name));
        }
        return process(end.reset().name(name));
    }

    /**
     Removes the last Element from the stack, hits onNodeClosed, and then returns it.
     * @return
     */
    HtmlElement pop() {
        int size = stack.size();
        HtmlElement removed = stack.remove(size - 1);
        onNodeClosed(removed);
        return removed;
    }

    /**
     Adds the specified Element to the end of the stack, and hits onNodeInserted.
     * @param element
     */
    final void push(HtmlElement element) {
        stack.add(element);
        onNodeInserted(element);
    }

    /**
     Get the current element (last on the stack). If all items have been removed, returns the document instead
     (which might not actually be on the stack; use stack.size() == 0 to test if required.
     @return the last element on the stack, if any; or the root document
     */
    HtmlElement currentElement() {
        int size = stack.size();
        return size > 0 ? stack.get(size-1) : doc;
    }

    /**
     Checks if the Current Element's normal name equals the supplied name, in the HTML namespace.
     @param normalName name to check
     @return true if there is a current element on the stack, and its name equals the supplied
     */
    boolean currentElementIs(String normalName) {
        if (stack.size() == 0)
            return false;
        HtmlElement current = currentElement();
        return current != null && current.normalName().equals(normalName)
                && current.tag().namespace().equals(NamespaceHtml);
    }

    /**
     Checks if the Current Element's normal name equals the supplied name, in the specified namespace.
     @param normalName name to check
     @param namespace the namespace
     @return true if there is a current element on the stack, and its name equals the supplied
     */
    boolean currentElementIs(String normalName, String namespace) {
        if (stack.size() == 0)
            return false;
        HtmlElement current = currentElement();
        return current != null && current.normalName().equals(normalName)
                && current.tag().namespace().equals(namespace);
    }

    /**
     * If the parser is tracking errors, add an error at the current position.
     * @param msg error message
     */
    void error(String msg) {
        error(msg, (Object[]) null);
    }

    /**
     * If the parser is tracking errors, add an error at the current position.
     * @param msg error message template
     * @param args template arguments
     */
    void error(String msg, Object... args) {
        ParseErrorList errors = parser.getErrors();
        if (errors.canAddError())
            errors.add(new ParseError(reader, msg, args));
    }

    Tag tagFor(String tagName, String normalName, String namespace, ParseSettings settings) {
        return tagSet.valueOf(tagName, normalName, namespace, settings.preserveTagCase());
    }

    Tag tagFor(Token.Tag token) {
        return tagSet.valueOf(token.name(), token.normalName, defaultNamespace(), settings.preserveTagCase());
    }

    /**
     Gets the default namespace for this TreeBuilder
     * @return the default namespace
     */
    String defaultNamespace() {
        return NamespaceHtml;
    }

    TagSet defaultTagSet() {
        return TagSet.Html();
    }

    /**
     Called by implementing TreeBuilders when a node has been inserted. This implementation includes optionally tracking
     the source range of the node.  @param node the node that was just inserted
     */
    void onNodeInserted(zenzai.nodes.Node node) {
        trackNodePosition(node, true);

        if (nodeListener != null)
            nodeListener.head(node, stack.size());
    }

    /**
     Called by implementing TreeBuilders when a node is explicitly closed. This implementation includes optionally
     tracking the closing source range of the node.  @param node the node being closed
     */
    void onNodeClosed(zenzai.nodes.Node node) {
        trackNodePosition(node, false);

        if (nodeListener != null)
            nodeListener.tail(node, stack.size());
    }

    void trackNodePosition(zenzai.nodes.Node node, boolean isStart) {
        if (!trackSourceRange) return;

        final Token token = currentToken;
        int startPos = token.startPos();
        int endPos = token.endPos();

        // handle implicit element open / closes.
        if (node instanceof HtmlElement) {
            final HtmlElement el = (HtmlElement) node;
            if (token.isEOF()) {
                if (el.endSourceRange().isTracked())
                    return; // /body and /html are left on stack until EOF, don't reset them
                startPos = endPos = reader.pos();
            } else if (isStart) { // opening tag
                if  (!token.isStartTag() || !el.normalName().equals(token.asStartTag().normalName)) {
                    endPos = startPos;
                }
            } else { // closing tag
                if (!el.tag().isEmpty() && !el.tag().isSelfClosing()) {
                    if (!token.isEndTag() || !el.normalName().equals(token.asEndTag().normalName)) {
                        endPos = startPos;
                    }
                }
            }
        }

        HtmlRange.Position startPosition = new HtmlRange.Position
                (startPos, reader.lineNumber(startPos), reader.columnNumber(startPos));
        HtmlRange.Position endPosition = new HtmlRange.Position
                (endPos, reader.lineNumber(endPos), reader.columnNumber(endPos));
        HtmlRange range = new HtmlRange(startPosition, endPosition);
        node.attributes().userData(isStart ? SharedConstants.RangeKey : SharedConstants.EndRangeKey, range);
    }
}
