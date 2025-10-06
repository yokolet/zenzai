package nokogiri.internals.html.select;

import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import nokogiri.internals.html.helper.Validate;
import nokogiri.internals.html.nodes.Element;

public class Selector {
    // not instantiable
    private Selector() {}

    /**
     Find Elements matching the CSS query.

     @param query CSS selector
     @param root root element to descend into
     @return matching elements, empty if none
     @throws nokogiri.internals.html.select.Selector.SelectorParseException (unchecked) on an invalid CSS query.
     */
    public static Elements select(String query, Element root) {
        Validate.notEmpty(query);
        return select(evaluatorOf(query), root);
    }

    /**
     Find Elements matching the Evaluator.

     @param evaluator CSS Evaluator
     @param root root (context) element to start from
     @return matching elements, empty if none
     */
    public static Elements select(Evaluator evaluator, Element root) {
        Validate.notNull(evaluator);
        Validate.notNull(root);
        return Collector.collect(evaluator, root);
    }

    /**
     Finds a Stream of elements matching the CSS query.

     @param query CSS selector
     @param root root element to descend into
     @return a Stream of matching elements, empty if none
     @throws nokogiri.internals.html.select.Selector.SelectorParseException (unchecked) on an invalid CSS query.
     @since 1.19.1
     */
    public static Stream<Element> selectStream(String query, Element root) {
        nokogiri.internals.html.helper.Validate.notEmpty(query);
        return selectStream(evaluatorOf(query), root);
    }

    /**
     Finds a Stream of elements matching the evaluator.

     @param evaluator CSS selector
     @param root root element to descend into
     @return matching elements, empty if none
     @since 1.19.1
     */
    public static Stream<Element> selectStream(Evaluator evaluator, Element root) {
        nokogiri.internals.html.helper.Validate.notNull(evaluator);
        nokogiri.internals.html.helper.Validate.notNull(root);
        return Collector.stream(evaluator, root);
    }

    /**
     Find the first Element that matches the query.

     @param cssQuery CSS selector
     @param root root element to descend into
     @return the matching element, or <b>null</b> if none.
     */
    public static @Nullable Element selectFirst(String cssQuery, Element root) {
        nokogiri.internals.html.helper.Validate.notEmpty(cssQuery);
        return Collector.findFirst(evaluatorOf(cssQuery), root);
    }

    /**
     Parse a CSS query into an Evaluator. If you are evaluating the same query repeatedly, it may be more efficient to
     parse it once and reuse the Evaluator.

     @param css CSS query
     @return Evaluator
     @see nokogiri.internals.html.select.Selector selector query syntax
     @throws nokogiri.internals.html.select.Selector.SelectorParseException if the CSS query is invalid
     @since 1.21.1
     */
    public static Evaluator evaluatorOf(String css) {
        return QueryParser.parse(css);
    }

    public static class SelectorParseException extends IllegalStateException {
        private static final long serialVersionUID = 1L;

        public SelectorParseException(String msg) {
            super(msg);
        }

        public SelectorParseException(String msg, Object... msgArgs) {
            super(String.format(msg, msgArgs));
        }

        public SelectorParseException(Throwable cause, String msg, Object... msgArgs) {
            super(String.format(msg, msgArgs), cause);
        }
    }
}
