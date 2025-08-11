package zenzai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMessage {
    @Test
    public void testHelloWorld() {
        assertEquals("hello world", Message.getHelloWorld());
    }

    @Test
    public void testNumber() {
        assertEquals(10, Message.getNumber10());
    }
}
