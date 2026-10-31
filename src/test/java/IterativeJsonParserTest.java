import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Tag("bonus")
@Timeout(20)
class IterativeJsonParserTest {

    private static Object parse(String json) {
        return IterativeJsonParser.parse(json);
    }

    // ----- same results as the recursive parser ------------------------------------------

    @Test
    @DisplayName("bonus: simple values")
    void simpleValues() {
        assertEquals("hi", parse("\"hi\""));
        assertEquals(Integer.valueOf(-5), parse(" -5 "));
        assertEquals(Double.valueOf(2.5), parse("2.5"));
        assertEquals(Boolean.TRUE, parse("true"));
        assertNull(parse("null"));
    }

    @Test
    @DisplayName("bonus: empty object and array, with exactly HashMap and ArrayList")
    void emptyContainers() {
        assertEquals(HashMap.class, parse("{}").getClass());
        assertEquals(java.util.ArrayList.class, parse("[]").getClass());
        assertTrue(TestUtil.asMap(parse(" { } ")).isEmpty());
        assertTrue(TestUtil.asList(parse(" [ ] ")).isEmpty());
    }

    @Test
    @DisplayName("bonus: object with simple values")
    void simpleObject() {
        Map<String, Object> m = TestUtil.asMap(parse("{\"a\":1,\"b\":\"x\",\"c\":null,\"d\":[true,false]}"));
        assertEquals(4, m.size());
        assertEquals(Integer.valueOf(1), m.get("a"));
        assertEquals("x", m.get("b"));
        assertTrue(m.containsKey("c"));
        assertNull(m.get("c"));
        assertEquals(List.of(true, false), m.get("d"));
    }

    @Test
    @DisplayName("bonus: nested arrays and objects")
    void nested() {
        Object result = parse("{\"a\":[{\"b\":[1,{\"c\":null}]},[[2]]],\"d\":{\"e\":{}},\"f\":[]}");
        Map<String, Object> m = TestUtil.asMap(result);
        List<Object> a = TestUtil.asList(m.get("a"));
        assertEquals(2, a.size());
        Map<String, Object> first = TestUtil.asMap(a.get(0));
        List<Object> b = TestUtil.asList(first.get("b"));
        assertEquals(Integer.valueOf(1), b.get(0));
        assertTrue(TestUtil.asMap(b.get(1)).containsKey("c"));
        assertEquals(List.of(List.of(2)), a.get(1));
        assertTrue(TestUtil.asMap(TestUtil.asMap(m.get("d")).get("e")).isEmpty());
        assertTrue(TestUtil.asList(m.get("f")).isEmpty());
    }

    @Test
    @DisplayName("bonus: duplicate keys are rejected")
    void duplicateKeys() {
        TestUtil.assertMalformedIterative("{\"a\":1,\"a\":2}");
        TestUtil.assertMalformedIterative("{\"x\":{\"a\":1,\"b\":2,\"a\":3}}");
    }

    @Test
    @DisplayName("bonus: gives the same result as the recursive parser on a mixed document")
    void sameAsRecursive() {
        String json = " [ {\"k\" : [1, 2.5, \"s\", null, true, {\"z\": {}}], \"e\": []}, [], {}, [[[]]], \"t\" ] ";
        assertEquals(JsonParser.parse(json), parse(json));
    }

    @Test
    @DisplayName("bonus: gives the same result as the recursive parser on the log file")
    void sameAsRecursiveOnLogs() throws IOException {
        String text = Files.readString(Path.of("data/logs.json"));
        assertEquals(JsonParser.parse(text), parse(text));
        String compact = Files.readString(Path.of("data/logs.min.json"));
        assertEquals(JsonParser.parse(compact), parse(compact));
    }

    // ----- the whole point: very deep nesting --------------------------------------------

    @Test
    @DisplayName("bonus: arrays nested 100,000 levels deep (a recursive parser overflows the call stack)")
    void deepArrays() {
        int depth = 100_000;
        Object current = parse("[".repeat(depth) + "]".repeat(depth));
        int levels = 0;
        while (true) {
            List<Object> list = TestUtil.asList(current);
            levels++;
            if (list.isEmpty()) {
                break;
            }
            assertEquals(1, list.size());
            current = list.get(0);
        }
        assertEquals(depth, levels);
    }

    @Test
    @DisplayName("bonus: objects nested 50,000 levels deep")
    void deepObjects() {
        int depth = 50_000;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append("{\"k\":");
        }
        sb.append("null");
        for (int i = 0; i < depth; i++) {
            sb.append('}');
        }
        Object current = parse(sb.toString());
        int levels = 0;
        while (current != null) {
            Map<String, Object> m = TestUtil.asMap(current);
            levels++;
            assertEquals(1, m.size());
            current = m.get("k");
        }
        assertEquals(depth, levels);
    }

    // ----- malformed input ---------------------------------------------------------------

    @Test
    @DisplayName("bonus errors: malformed objects")
    void malformedObjects() {
        TestUtil.assertMalformedIterative("{");
        TestUtil.assertMalformedIterative("{\"a\"");
        TestUtil.assertMalformedIterative("{\"a\":");
        TestUtil.assertMalformedIterative("{\"a\":1");
        TestUtil.assertMalformedIterative("{\"a\":1,}");
        TestUtil.assertMalformedIterative("{,}");
        TestUtil.assertMalformedIterative("{\"a\" 1}");
        TestUtil.assertMalformedIterative("{\"a\":1 \"b\":2}");
        TestUtil.assertMalformedIterative("{a:1}");
        TestUtil.assertMalformedIterative("{\"a\":1]");
        TestUtil.assertMalformedIterative("{\"a\":}");
        TestUtil.assertMalformedIterative("{\"a\":{\"b\":1}");
    }

    @Test
    @DisplayName("bonus errors: malformed arrays")
    void malformedArrays() {
        TestUtil.assertMalformedIterative("[");
        TestUtil.assertMalformedIterative("[1");
        TestUtil.assertMalformedIterative("[1,");
        TestUtil.assertMalformedIterative("[1,]");
        TestUtil.assertMalformedIterative("[,]");
        TestUtil.assertMalformedIterative("[1 2]");
        TestUtil.assertMalformedIterative("[1}");
        TestUtil.assertMalformedIterative("[[1,2]");
        TestUtil.assertMalformedIterative("[{\"a\":1]");
    }

    @Test
    @DisplayName("bonus errors: extra content, empty input, bad values")
    void malformedOther() {
        TestUtil.assertMalformedIterative("");
        TestUtil.assertMalformedIterative("[] []");
        TestUtil.assertMalformedIterative("{}}");
        TestUtil.assertMalformedIterative("[1]]");
        TestUtil.assertMalformedIterative("[tru]");
        TestUtil.assertMalformedIterative("[01]");
        TestUtil.assertMalformedIterative("@");
    }

    @Test
    @DisplayName("bonus errors: unclosed nesting 100,000 levels deep still gives a JsonParseException")
    void unclosedDeep() {
        TestUtil.assertMalformedIterative("[".repeat(100_000));
    }

    // ----- the rule: no recursion --------------------------------------------------------

    @Test
    @DisplayName("bonus rules: IterativeJsonParser does not call parseValue, parseObject, or parseArray")
    void noRecursionInSource() throws IOException {
        String source = Files.readString(Path.of("src/main/java/IterativeJsonParser.java"));
        String code = source.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("(?m)//.*$", " ");
        int parseValueMentions = code.split("parseValue\\(", -1).length - 1;
        assertEquals(1, parseValueMentions,
                "parseValue( should appear exactly once in IterativeJsonParser.java: its own declaration");
        assertEquals(0, code.split("parseObject\\(", -1).length - 1, "do not call parseObject");
        assertEquals(0, code.split("parseArray\\(", -1).length - 1, "do not call parseArray");
        assertTrue(!code.contains("super.parseValue"), "do not call super.parseValue");
    }
}
