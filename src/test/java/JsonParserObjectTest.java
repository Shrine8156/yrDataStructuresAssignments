import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(5)
class JsonParserObjectTest {

    private static Map<String, Object> parseObject(String json) {
        Object result = JsonParser.parse(json);
        assertTrue(result instanceof Map, "parse should return a Map for an object, but returned: " + result);
        return TestUtil.asMap(result);
    }

    // ----- the basics --------------------------------------------------------------------

    @Test
    @DisplayName("object: empty {}")
    void emptyObject() {
        Map<String, Object> m = parseObject("{}");
        assertTrue(m.isEmpty());
    }

    @Test
    @DisplayName("object: the result is exactly a java.util.HashMap")
    void resultIsHashMap() {
        assertEquals(HashMap.class, JsonParser.parse("{}").getClass());
        assertEquals(HashMap.class, JsonParser.parse("{\"a\":1}").getClass());
    }

    @Test
    @DisplayName("object: whitespace inside an empty object")
    void emptyObjectWithWhitespace() {
        assertTrue(parseObject("{ }").isEmpty());
        assertTrue(parseObject("  {\n\t}  ").isEmpty());
    }

    @Test
    @DisplayName("object: one key with a string value")
    void singlePair() {
        Map<String, Object> m = parseObject("{\"name\":\"Ada\"}");
        assertEquals(1, m.size());
        assertEquals("Ada", m.get("name"));
    }

    @Test
    @DisplayName("object: several keys with every kind of simple value")
    void allSimpleValueTypes() {
        Map<String, Object> m = parseObject(
                "{\"s\":\"text\",\"i\":7,\"d\":2.5,\"neg\":-3,\"t\":true,\"f\":false,\"n\":null}");
        assertEquals(7, m.size());
        assertEquals("text", m.get("s"));
        assertEquals(Integer.valueOf(7), m.get("i"));
        assertEquals(Double.valueOf(2.5), m.get("d"));
        assertEquals(Integer.valueOf(-3), m.get("neg"));
        assertEquals(Boolean.TRUE, m.get("t"));
        assertEquals(Boolean.FALSE, m.get("f"));
    }

    @Test
    @DisplayName("object: a null value is stored, and its key is still in the map")
    void nullValueKeepsKey() {
        Map<String, Object> m = parseObject("{\"a\":null,\"b\":1}");
        assertEquals(2, m.size());
        assertTrue(m.containsKey("a"), "the key \"a\" must be present even though its value is null");
        assertNull(m.get("a"));
    }

    @Test
    @DisplayName("object: whitespace allowed everywhere")
    void whitespaceEverywhere() {
        Map<String, Object> m = parseObject(" {\n  \"a\" : 1 ,\r\n\t\"b\"\n:\n 2\n }\n");
        assertEquals(2, m.size());
        assertEquals(Integer.valueOf(1), m.get("a"));
        assertEquals(Integer.valueOf(2), m.get("b"));
    }

    @Test
    @DisplayName("object: keys can be empty, contain spaces, or contain escapes")
    void unusualKeys() {
        // JSON text:  {"":0, "two words":1, "quote\"d":2, "tab\there":3}
        Map<String, Object> m = parseObject("{\"\":0,\"two words\":1,\"quote\\\"d\":2,\"tab\\there\":3}");
        assertEquals(4, m.size());
        assertEquals(Integer.valueOf(0), m.get(""));
        assertEquals(Integer.valueOf(1), m.get("two words"));
        assertEquals(Integer.valueOf(2), m.get("quote\"d"));
        assertEquals(Integer.valueOf(3), m.get("tab\there"));
    }

    @Test
    @DisplayName("object: string values may contain , : { } [ ] characters")
    void structuralCharactersInsideStrings() {
        Map<String, Object> m = parseObject("{\"k\":\"a,b:c{d}[e]\",\"j\":\"}\"}");
        assertEquals(2, m.size());
        assertEquals("a,b:c{d}[e]", m.get("k"));
        assertEquals("}", m.get("j"));
    }

    @Test
    @DisplayName("object: a duplicate key is rejected")
    void duplicateKeyRejected() {
        TestUtil.assertMalformed("{\"a\":1,\"b\":5,\"a\":2}");
        TestUtil.assertMalformed("{\"a\":1,\"a\":1}");
        TestUtil.assertMalformed("{\"a\":null,\"a\":null}");
    }

    @Test
    @DisplayName("object: duplicate key error points at the second key")
    void duplicateKeyPosition() {
        JsonParseException e = org.junit.jupiter.api.Assertions.assertThrows(JsonParseException.class,
                () -> JsonParser.parse("{\"a\":1,\"a\":2}"));
        assertEquals(7, e.getPosition());
    }

    @Test
    @DisplayName("object: the same key in different objects is fine")
    void sameKeyInDifferentObjects() {
        Map<String, Object> m = parseObject("{\"a\":{\"a\":1},\"b\":{\"a\":2}}");
        assertEquals(2, m.size());
        assertEquals(Integer.valueOf(1), TestUtil.asMap(m.get("a")).get("a"));
        assertEquals(Integer.valueOf(2), TestUtil.asMap(m.get("b")).get("a"));
    }

    // ----- nesting (the recursion) -------------------------------------------------------

    @Test
    @DisplayName("object: an object inside an object")
    void nestedObject() {
        Map<String, Object> m = parseObject("{\"outer\":{\"inner\":{\"x\":1}}}");
        Map<String, Object> outer = TestUtil.asMap(m.get("outer"));
        Map<String, Object> inner = TestUtil.asMap(outer.get("inner"));
        assertEquals(Integer.valueOf(1), inner.get("x"));
        assertEquals(HashMap.class, outer.getClass());
        assertEquals(HashMap.class, inner.getClass());
    }

    @Test
    @DisplayName("object: sibling nested objects stay separate")
    void siblingObjects() {
        Map<String, Object> m = parseObject("{\"a\":{\"v\":1},\"b\":{\"v\":2},\"c\":{}}");
        assertEquals(3, m.size());
        assertEquals(Integer.valueOf(1), TestUtil.asMap(m.get("a")).get("v"));
        assertEquals(Integer.valueOf(2), TestUtil.asMap(m.get("b")).get("v"));
        assertTrue(TestUtil.asMap(m.get("c")).isEmpty());
        assertNotSame(m.get("a"), m.get("b"));
    }

    @Test
    @DisplayName("object: values after a nested object are not lost")
    void valuesAfterNestedObject() {
        Map<String, Object> m = parseObject("{\"first\":{\"x\":1},\"second\":2,\"third\":{\"y\":3},\"fourth\":4}");
        assertEquals(4, m.size());
        assertEquals(Integer.valueOf(2), m.get("second"));
        assertEquals(Integer.valueOf(4), m.get("fourth"));
    }

    @Test
    @DisplayName("object: an array inside an object")
    void objectContainingArray() {
        Map<String, Object> m = parseObject("{\"nums\":[1,2,3],\"after\":true}");
        assertEquals(List.of(1, 2, 3), m.get("nums"));
        assertEquals(Boolean.TRUE, m.get("after"));
    }

    @Test
    @DisplayName("object: objects and arrays nested several levels")
    void mixedNesting() {
        Map<String, Object> m = parseObject("{\"a\":[{\"b\":[1,{\"c\":null}]}]}");
        List<Object> a = TestUtil.asList(m.get("a"));
        Map<String, Object> first = TestUtil.asMap(a.get(0));
        List<Object> b = TestUtil.asList(first.get("b"));
        assertEquals(Integer.valueOf(1), b.get(0));
        Map<String, Object> c = TestUtil.asMap(b.get(1));
        assertTrue(c.containsKey("c"));
        assertNull(c.get("c"));
    }

    @Test
    @DisplayName("object: 1,000 keys")
    void manyKeys() {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < 1000; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("\"key").append(i).append("\":").append(i * 2);
        }
        sb.append('}');
        Map<String, Object> m = parseObject(sb.toString());
        assertEquals(1000, m.size());
        assertEquals(Integer.valueOf(0), m.get("key0"));
        assertEquals(Integer.valueOf(998), m.get("key499"));
        assertEquals(Integer.valueOf(1998), m.get("key999"));
        assertFalse(m.containsKey("key1000"));
    }

    // ----- malformed objects -------------------------------------------------------------

    @Test
    @DisplayName("object errors: missing closing brace")
    void missingClosingBrace() {
        TestUtil.assertMalformed("{");
        TestUtil.assertMalformed("{\"a\"");
        TestUtil.assertMalformed("{\"a\":");
        TestUtil.assertMalformed("{\"a\":1");
        TestUtil.assertMalformed("{\"a\":1,");
        TestUtil.assertMalformed("{\"a\":{\"b\":1}");
    }

    @Test
    @DisplayName("object errors: wrong or extra closing bracket")
    void wrongCloser() {
        TestUtil.assertMalformed("{\"a\":1]");
        TestUtil.assertMalformed("{\"a\":1}}");
        TestUtil.assertMalformed("{\"a\":[1}");
    }

    @Test
    @DisplayName("object errors: missing colon or value")
    void missingColonOrValue() {
        TestUtil.assertMalformed("{\"a\" 1}");
        TestUtil.assertMalformed("{\"a\"}");
        TestUtil.assertMalformed("{\"a\",1}");
        TestUtil.assertMalformed("{\"a\":}");
        TestUtil.assertMalformed("{\"a\":,\"b\":1}");
        TestUtil.assertMalformed("{\"a\"::1}");
    }

    @Test
    @DisplayName("object errors: missing comma")
    void missingComma() {
        TestUtil.assertMalformed("{\"a\":1 \"b\":2}");
        TestUtil.assertMalformed("{\"a\":1\"b\":2}");
        TestUtil.assertMalformed("{\"a\":{} \"b\":2}");
    }

    @Test
    @DisplayName("object errors: extra commas")
    void extraCommas() {
        TestUtil.assertMalformed("{\"a\":1,}");
        TestUtil.assertMalformed("{,}");
        TestUtil.assertMalformed("{,\"a\":1}");
        TestUtil.assertMalformed("{\"a\":1,,\"b\":2}");
        TestUtil.assertMalformed("{\"a\":{\"b\":1,},\"c\":2}");
    }

    @Test
    @DisplayName("object errors: keys must be strings")
    void badKeys() {
        TestUtil.assertMalformed("{a:1}");
        TestUtil.assertMalformed("{1:2}");
        TestUtil.assertMalformed("{'a':1}");
        TestUtil.assertMalformed("{null:1}");
        TestUtil.assertMalformed("{true:1}");
        TestUtil.assertMalformed("{{}:1}");
        TestUtil.assertMalformed("{[]:1}");
    }

    @Test
    @DisplayName("object errors: bad values and bad nested content")
    void badValues() {
        TestUtil.assertMalformed("{\"a\":tru}");
        TestUtil.assertMalformed("{\"a\":01}");
        TestUtil.assertMalformed("{\"a\":\"unterminated}");
        TestUtil.assertMalformed("{\"a\":[1,2}");
        TestUtil.assertMalformed("{\"a\":{\"b\" 1}}");
    }
}
