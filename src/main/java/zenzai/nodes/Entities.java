package zenzai.nodes;

import zenzai.helper.Validate;
import zenzai.parser.CharacterReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static zenzai.nodes.Entities.EscapeMode.extended;

public class Entities {
    private static final int empty = -1;
    private static final String emptyName = "";
    static final int codepointRadix = 36;
    private static final char[] codeDelims = {',', ';'};
    private static final HashMap<String, String> multipoints = new HashMap<>(); // name -> multiple character references
    private static final int BaseCount = 106;
    private static final ArrayList<String> baseSorted = new ArrayList<>(BaseCount); // names sorted longest first, for prefix matching

    public enum EscapeMode {
        /**
         * Restricted entities suitable for XHTML output: lt, gt, amp, and quot only.
         */
        xhtml(EntitiesData.xmlPoints, 4),
        /**
         * Default HTML output entities.
         */
        base(EntitiesData.basePoints, 106),
        /**
         * Complete HTML entities.
         */
        extended(EntitiesData.fullPoints, 2125);

        static {
            // sort the base names by length, for prefix matching
            Collections.addAll(baseSorted, base.nameKeys);
            baseSorted.sort((a, b) -> b.length() - a.length());
        }

        // table of named references to their codepoints. sorted so we can binary search. built by BuildEntities.
        private String[] nameKeys;
        private int[] codeVals; // limitation is the few references with multiple characters; those go into multipoints.

        // table of codepoints to named entities.
        private int[] codeKeys; // we don't support multicodepoints to single named value currently
        private String[] nameVals;

        EscapeMode(String file, int size) {
            load(this, file, size);
        }

        int codepointForName(final String name) {
            int index = Arrays.binarySearch(nameKeys, name);
            return index >= 0 ? codeVals[index] : empty;
        }

        String nameForCodepoint(final int codepoint) {
            final int index = Arrays.binarySearch(codeKeys, codepoint);
            if (index >= 0) {
                // the results are ordered so lower case versions of same codepoint come after uppercase, and we prefer to emit lower
                // (and binary search for same item with multi results is undefined
                return (index < nameVals.length - 1 && codeKeys[index + 1] == codepoint) ?
                        nameVals[index + 1] : nameVals[index];
            }
            return emptyName;
        }
    }

    /**
     * Check if the input is a known named entity
     *
     * @param name the possible entity name (e.g. "lt" or "amp")
     * @return true if a known named entity
     */
    public static boolean isNamedEntity(final String name) {
        return extended.codepointForName(name) != empty;
    }

    /**
     * Check if the input is a known named entity in the base entity set.
     *
     * @param name the possible entity name (e.g. "lt" or "amp")
     * @return true if a known named entity in the base set
     * @see #isNamedEntity(String)
     */
    public static boolean isBaseNamedEntity(final String name) {
        return EscapeMode.base.codepointForName(name) != empty;
    }

    /**
     Finds the longest base named entity that is a prefix of the input. That is, input "notit" would return "not".

     @return longest entity name that is a prefix of the input, or "" if no entity matches
     */
    public static String findPrefix(String input) {
        for (String name : baseSorted) {
            if (input.startsWith(name)) return name;
        }
        return emptyName;
        // if perf critical, could look at using a Trie vs a scan
    }

    public static int codepointsForName(final String name, final int[] codepoints) {
        String val = multipoints.get(name);
        if (val != null) {
            codepoints[0] = val.codePointAt(0);
            codepoints[1] = val.codePointAt(1);
            return 2;
        }
        int codepoint = extended.codepointForName(name);
        if (codepoint != empty) {
            codepoints[0] = codepoint;
            return 1;
        }
        return 0;
    }

    private Entities() {
    }

    private static void load(EscapeMode e, String pointsData, int size) {
        e.nameKeys = new String[size];
        e.codeVals = new int[size];
        e.codeKeys = new int[size];
        e.nameVals = new String[size];

        int i = 0;
        try (CharacterReader reader = new CharacterReader(pointsData)) {
            while (!reader.isEmpty()) {
                // NotNestedLessLess=10913,824;1887&

                final String name = reader.consumeTo('=');
                reader.advance();
                final int cp1 = Integer.parseInt(reader.consumeToAny(codeDelims), codepointRadix);
                final char codeDelim = reader.current();
                reader.advance();
                final int cp2;
                if (codeDelim == ',') {
                    cp2 = Integer.parseInt(reader.consumeTo(';'), codepointRadix);
                    reader.advance();
                } else {
                    cp2 = empty;
                }
                final String indexS = reader.consumeTo('&');
                final int index = Integer.parseInt(indexS, codepointRadix);
                reader.advance();

                e.nameKeys[i] = name;
                e.codeVals[i] = cp1;
                e.codeKeys[index] = cp1;
                e.nameVals[index] = name;

                if (cp2 != empty) {
                    multipoints.put(name, new String(new int[]{cp1, cp2}, 0, 2));
                }
                i++;
            }

            Validate.isTrue(i == size, "Unexpected count of entities loaded");
        }
    }
}
