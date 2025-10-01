package zenzai.nodes;

import org.junit.jupiter.api.Test;
import zenzai.parser.Parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FormElementTest {
    @Test
    public void testFormElements() {
        String html = "<form id=1><button id=1><fieldset id=2 /><input id=3><keygen id=4><object id=5><output id=6>" +
                "<select id=7><option></select><textarea id=8><p id=9>";
        Document doc = Parser.parse(html, "");
        FormElement form = (FormElement) doc.getElementsByTag("form").first();
        assertEquals(8, form.elements().size());
    }
}
