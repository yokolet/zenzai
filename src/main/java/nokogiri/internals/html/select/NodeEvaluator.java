package nokogiri.internals.html.select;

import nokogiri.internals.html.internal.StringUtil;
import nokogiri.internals.html.nodes.Element;
import nokogiri.internals.html.nodes.LeafNode;
import nokogiri.internals.html.nodes.Node;

import java.util.regex.Pattern;

import static nokogiri.internals.html.internal.Normalizer.lowerCase;
import static nokogiri.internals.html.internal.StringUtil.normaliseWhitespace;

abstract class NodeEvaluator extends Evaluator {

    @Override
    public boolean matches(Element root, Element element) {
        return evaluateMatch(element);
    }

    @Override boolean matches(Element root, LeafNode leaf) {
        return evaluateMatch(leaf);
    }

    abstract boolean evaluateMatch(Node node);
    
    @Override boolean wantsNodes() {
        return true;
    }

    static class InstanceType extends NodeEvaluator {
        final java.lang.Class<? extends Node> type;
        final String selector;

        InstanceType(java.lang.Class<? extends Node> type, String selector) {
            super();
            this.type = type;
            this.selector = "::" + selector;
        }

        @Override
        boolean evaluateMatch(Node node) {
            return type.isInstance(node);
        }

        @Override
        protected int cost() {
            return 1;
        }

        @Override
        public String toString() {
            return selector;
        }
    }

    static class ContainsValue extends NodeEvaluator {
        private final String searchText;

        public ContainsValue(String searchText) {
            this.searchText = lowerCase(normaliseWhitespace(searchText));
        }

        @Override
        boolean evaluateMatch(Node node) {
            return lowerCase(node.nodeValue()).contains(searchText);
        }

        @Override
        protected int cost() {
            return 6;
        }

        @Override
        public String toString() {
            return String.format(":contains(%s)", searchText);
        }
    }

    /**
     Matches nodes with no value or only whitespace.
     */
    static class BlankValue extends NodeEvaluator {

        @Override
        boolean evaluateMatch(Node node) {
            return StringUtil.isBlank(node.nodeValue());
        }

        @Override
        protected int cost() {
            return 4;
        }

        @Override
        public String toString() {
            return ":blank";
        }
    }

    static class MatchesValue extends NodeEvaluator {
        private final Pattern pattern;

        protected MatchesValue(Pattern pattern) {
            this.pattern = pattern;
        }

        @Override
        boolean evaluateMatch(Node node) {
            return pattern.matcher(node.nodeValue()).find();
        }

        @Override
        protected int cost() {
            return 8;
        }

        @Override
        public String toString() {
            return String.format(":matches(%s)", pattern);
        }
    }
}
