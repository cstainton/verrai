package dev.verrai.api.wire;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class JsonWriterTest {

    @Test
    public void testEmptyObject() {
        StringBuilder sb = new StringBuilder();
        JsonWriter writer = new JsonWriterImpl(sb);
        writer.beginObject();
        writer.endObject();
        assertEquals("{}", sb.toString());
    }

    @Test
    public void testSimpleObject() {
        StringBuilder sb = new StringBuilder();
        JsonWriter writer = new JsonWriterImpl(sb);
        writer.beginObject();
        writer.name("foo");
        writer.value("bar");
        writer.name("baz");
        writer.value(123);
        writer.endObject();
        assertEquals("{\"foo\":\"bar\",\"baz\":123}", sb.toString());
    }

    @Test
    public void testArray() {
        StringBuilder sb = new StringBuilder();
        JsonWriter writer = new JsonWriterImpl(sb);
        writer.beginArray();
        writer.value("a");
        writer.value("b");
        writer.value(1);
        writer.endArray();
        assertEquals("[\"a\",\"b\",1]", sb.toString());
    }

    @Test
    public void testNested() {
        StringBuilder sb = new StringBuilder();
        JsonWriter writer = new JsonWriterImpl(sb);
        writer.beginObject();
        writer.name("arr");
        writer.beginArray();
        writer.value(1);
        writer.beginObject();
        writer.name("x");
        writer.value("y");
        writer.endObject();
        writer.endArray();
        writer.endObject();
        assertEquals("{\"arr\":[1,{\"x\":\"y\"}]}", sb.toString());
    }

    @Test
    public void testEscaping() {
        StringBuilder sb = new StringBuilder();
        JsonWriter writer = new JsonWriterImpl(sb);
        writer.beginObject();
        writer.name("quote\"");
        writer.value("slash\\");
        writer.endObject();
        assertEquals("{\"quote\\\"\":\"slash\\\\\"}", sb.toString());
    }
}
