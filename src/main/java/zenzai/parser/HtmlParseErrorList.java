package zenzai.parser;

import java.util.ArrayList;

/**
 * A container for ParseErrors.
 *
 * @author Jonathan Hedley
 */
public class HtmlParseErrorList extends ArrayList<HtmlParseError>{
    private static final int INITIAL_CAPACITY = 16;
    private final int initialCapacity;
    private final int maxSize;

    HtmlParseErrorList(int initialCapacity, int maxSize) {
        super(initialCapacity);
        this.initialCapacity = initialCapacity;
        this.maxSize = maxSize;
    }

    /**
     Create a new ParseErrorList with the same settings, but no errors in the list
     @param copy initial and max size details to copy
     */
    HtmlParseErrorList(HtmlParseErrorList copy) {
        this(copy.initialCapacity, copy.maxSize);
    }

    boolean canAddError() {
        return size() < maxSize;
    }

    int getMaxSize() {
        return maxSize;
    }

    public static HtmlParseErrorList noTracking() {
        return new HtmlParseErrorList(0, 0);
    }

    public static HtmlParseErrorList tracking(int maxSize) {
        return new HtmlParseErrorList(INITIAL_CAPACITY, maxSize);
    }

    @Override
    public Object clone() {
        // all class fields are primitive, so native clone is enough.
        return super.clone();
    }
}
