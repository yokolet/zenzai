package zenzai.nodes;

import java.util.Objects;

import zenzai.internal.StringUtil;

import static zenzai.internal.SharedConstants.*;

public class HtmlRange {
    private static final Position UntrackedPos = new Position(-1, -1, -1);
    private final Position start, end;

    /**
     Creates a new Range with start and end Positions. Called by TreeBuilder when position tracking is on.
     * @param start the start position
     * @param end the end position
     */
    public HtmlRange(Position start, Position end) {
        this.start = start;
        this.end = end;
    }

    /**
     * Retrieves the source range for a given Node.
     *
     * @param node  the node to retrieve the position for
     * @param start if this is the starting range. {@code false} for Element end tags.
     * @return the Range, or the Untracked (-1) position if tracking is disabled.
     */
    static HtmlRange of(HtmlNode node, boolean start) {

        final String key = start ? RangeKey : EndRangeKey;
        if (!node.hasAttributes()) return Untracked;
        Object range = node.attributes().userData(key);
        return range != null ? (HtmlRange) range : Untracked;
    }

    /**
     Test if this source range was tracked during parsing.
     * @return true if this was tracked during parsing, false otherwise (and all fields will be {@code -1}).
     */
    public boolean isTracked() {
        return this != Untracked;
    }

    /** An untracked source range. */
    static final HtmlRange Untracked = new HtmlRange(UntrackedPos, UntrackedPos);

    /**
     A Position object tracks the character position in the original input source where a Node starts or ends. If you want to
     track these positions, tracking must be enabled in the Parser with
     {@link org.jsoup.parser.Parser#setTrackPosition(boolean)}.
     @see HtmlNode#sourceRange()
     */
    public static class Position {
        private final int pos, lineNumber, columnNumber;

        /**
         Create a new Position object. Called by the TreeBuilder if source position tracking is on.
         * @param pos position index
         * @param lineNumber line number
         * @param columnNumber column number
         */
        public Position(int pos, int lineNumber, int columnNumber) {
            this.pos = pos;
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
        }

        /**
         Gets the position index (0-based) of the original input source that this Position was read at. This tracks the
         total number of characters read into the source at this position, regardless of the number of preceding lines.
         * @return the position, or {@code -1} if untracked.
         */
        public int pos() {
            return pos;
        }

        /**
         Gets the line number (1-based) of the original input source that this Position was read at.
         * @return the line number, or {@code -1} if untracked.
         */
        public int lineNumber() {
            return lineNumber;
        }

        /**
         Gets the cursor number (1-based) of the original input source that this Position was read at. The cursor number
         resets to 1 on every new line.
         * @return the cursor number, or {@code -1} if untracked.
         */
        public int columnNumber() {
            return columnNumber;
        }

        /**
         Test if this position was tracked during parsing.
         * @return true if this was tracked during parsing, false otherwise (and all fields will be {@code -1}).
         */
        public boolean isTracked() {
            return this != UntrackedPos;
        }

        /**
         Gets a String presentation of this Position, in the format {@code line,column:pos}.
         * @return a String
         */
        @Override
        public String toString() {
            return lineNumber + "," + columnNumber + ":" + pos;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Position position = (Position) o;
            if (pos != position.pos) return false;
            if (lineNumber != position.lineNumber) return false;
            return columnNumber == position.columnNumber;
        }

        @Override
        public int hashCode() {
            return Objects.hash(pos, lineNumber, columnNumber);
        }
    }

    public static class AttributeRange {
        static final AttributeRange UntrackedAttr = new AttributeRange(HtmlRange.Untracked, HtmlRange.Untracked);

        private final HtmlRange nameRange;
        private final HtmlRange valueRange;

        /** Creates a new AttributeRange. Called during parsing by Token.StartTag. */
        public AttributeRange(HtmlRange nameRange, HtmlRange valueRange) {
            this.nameRange = nameRange;
            this.valueRange = valueRange;
        }

        /** Get the source range for the attribute's name. */
        public HtmlRange nameRange() {
            return nameRange;
        }

        /** Get the source range for the attribute's value. */
        public HtmlRange valueRange() {
            return valueRange;
        }

        /** Get a String presentation of this Attribute range, in the form
         {@code line,column:pos-line,column:pos=line,column:pos-line,column:pos} (name start - name end = val start - val end).
         . */
        @Override public String toString() {
            StringBuilder sb = StringUtil.borrowBuilder()
                    .append(nameRange)
                    .append('=')
                    .append(valueRange);
            return StringUtil.releaseBuilder(sb);
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AttributeRange that = (AttributeRange) o;

            if (!nameRange.equals(that.nameRange)) return false;
            return valueRange.equals(that.valueRange);
        }

        @Override public int hashCode() {
            return Objects.hash(nameRange, valueRange);
        }
    }
}
