import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(5)
class JsonParserArrayTest {

    private static List<Object> parseArray(String json) {
        Object result = JsonParser.parse(json);
        assertTrue(result instanceof List, "parse should return a List for an array, but returned: " + result);
        return TestUtil.asList(result);
    }

    // ----- the basics --------------------------------------------------------------------

    @Test
    @DisplayName("array: empty []")
    void emptyArray() {
        assertTrue(parseArray("[]").isEmpty());
        assertTrue(parseArray("[ ]").isEmpty());
        assertTrue(parseArray(" [\n\t] ").isEmpty());
    }

    @Test
    @DisplayName("array: the result is exactly a java.util.ArrayList")
    void resultIsArrayList() {
        assertEquals(ArrayList.class, JsonParser.parse("[]").getClass());
        assertEquals(ArrayList.class, JsonParser.parse("[1]").getClass());
    }

    @Test
    @DisplayName("array: one element")
    void oneElement() {
        List<Object> list = parseArray("[42]");
        assertEquals(1, list.size());
        assertEquals(Integer.valueOf(42), list.get(0));
    }

    @Test
    @DisplayName("array: numbers keep their order")
    void numbersInOrder() {
        assertEquals(List.of(3, 1, 2), parseArray("[3,1,2]"));
    }

    @Test
    @DisplayName("array: strings")
    void strings() {
        assertEquals(List.of("a", "b", "c"), parseArray("[\"a\",\"b\",\"c\"]"));
    }

    @Test
    @DisplayName("array: every kind of simple value, including null")
    void mixedSimpleValues() {
        List<Object> list = parseArray("[1,\"two\",3.5,true,false,null,-4]");
        assertEquals(7, list.size(), "a null element still counts toward the size");
        assertEquals(Integer.valueOf(1), list.get(0));
        assertEquals("two", list.get(1));
        assertEquals(Double.valueOf(3.5), list.get(2));
        assertEquals(Boolean.TRUE, list.get(3));
        assertEquals(Boolean.FALSE, list.get(4));
        assertNull(list.get(5));
        assertEquals(Integer.valueOf(-4), list.get(6));
    }

    @Test
    @DisplayName("array: whitespace allowed everywhere")
    void whitespaceEverywhere() {
        assertEquals(List.of(1, 2, 3), parseArray(" [ 1 ,\n\t2\r\n,3 \n] "));
    }

    @Test
    @DisplayName("array: strings may contain , [ ] characters")
    void structuralCharactersInsideStrings() {
        assertEquals(List.of("a,b", "]", "[x,y]"), parseArray("[\"a,b\",\"]\",\"[x,y]\"]"));
    }

    @Test
    @DisplayName("array: 500 elements stay in order")
    void manyElements() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 500; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(i);
        }
        sb.append(']');
        List<Object> list = parseArray(sb.toString());
        assertEquals(500, list.size());
        for (int i = 0; i < 500; i++) {
            assertEquals(Integer.valueOf(i), list.get(i));
        }
    }

    // ----- nesting (the recursion) -------------------------------------------------------

    @Test
    @DisplayName("array: arrays inside arrays")
    void nestedArrays() {
        List<Object> list = parseArray("[[1,2],[3,[4,5]],[]]");
        assertEquals(3, list.size());
        assertEquals(List.of(1, 2), list.get(0));
        List<Object> second = TestUtil.asList(list.get(1));
        assertEquals(Integer.valueOf(3), second.get(0));
        assertEquals(List.of(4, 5), second.get(1));
        assertTrue(TestUtil.asList(list.get(2)).isEmpty());
    }

    @Test
    @DisplayName("array: elements after a nested array are not lost")
    void elementsAfterNestedArray() {
        assertEquals(List.of(List.of(1), 2, List.of(3), 4), parseArray("[[1],2,[3],4]"));
    }

    @Test
    @DisplayName("array: sibling empty arrays are separate objects")
    void siblingArraysAreDistinct() {
        List<Object> list = parseArray("[[],[]]");
        assertEquals(2, list.size());
        assertNotSame(list.get(0), list.get(1));
    }

    @Test
    @DisplayName("array: objects inside an array")
    void arrayOfObjects() {
        List<Object> list = parseArray("[{\"id\":1},{\"id\":2,\"tag\":\"x\"},{}]");
        assertEquals(3, list.size());
        Map<String, Object> first = TestUtil.asMap(list.get(0));
        Map<String, Object> second = TestUtil.asMap(list.get(1));
        assertEquals(Integer.valueOf(1), first.get("id"));
        assertEquals(Integer.valueOf(2), second.get("id"));
        assertEquals("x", second.get("tag"));
        assertTrue(TestUtil.asMap(list.get(2)).isEmpty());
    }

    @Test
    @DisplayName("array: arrays and objects nested several levels")
    void mixedNesting() {
        List<Object> list = parseArray("[{\"a\":[1,{\"b\":[true]}]},[[null]]]");
        Map<String, Object> first = TestUtil.asMap(list.get(0));
        List<Object> a = TestUtil.asList(first.get("a"));
        assertEquals(Integer.valueOf(1), a.get(0));
        Map<String, Object> inner = TestUtil.asMap(a.get(1));
        assertEquals(List.of(true), inner.get("b"));
        List<Object> second = TestUtil.asList(list.get(1));
        List<Object> innerList = TestUtil.asList(second.get(0));
        assertEquals(1, innerList.size());
        assertNull(innerList.get(0));
    }

    @Test
    @DisplayName("array: nesting 500 levels deep")
    void deepNesting() {
        int depth = 500;
        String json = "[".repeat(depth) + "]".repeat(depth);
        Object current = JsonParser.parse(json);
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

    // ----- malformed arrays --------------------------------------------------------------

    @Test
    @DisplayName("array errors: missing closing bracket")
    void missingClosingBracket() {
        TestUtil.assertMalformed("[");
        TestUtil.assertMalformed("[1");
        TestUtil.assertMalformed("[1,");
        TestUtil.assertMalformed("[1,2");
        TestUtil.assertMalformed("[[1,2]");
        TestUtil.assertMalformed("[{\"a\":1}");
    }

    @Test
    @DisplayName("array errors: wrong or extra closing bracket")
    void wrongCloser() {
        TestUtil.assertMalformed("[1}");
        TestUtil.assertMalformed("[1,2]]");
        TestUtil.assertMalformed("]");
        TestUtil.assertMalformed("[{\"a\":1]");
    }

    @Test
    @DisplayName("array errors: missing comma")
    void missingComma() {
        TestUtil.assertMalformed("[1 2]");
        TestUtil.assertMalformed("[\"a\" \"b\"]");
        TestUtil.assertMalformed("[[1] [2]]");
        TestUtil.assertMalformed("[true false]");
    }

    @Test
    @DisplayName("array errors: extra commas")
    void extraCommas() {
        TestUtil.assertMalformed("[1,]");
        TestUtil.assertMalformed("[,]");
        TestUtil.assertMalformed("[,1]");
        TestUtil.assertMalformed("[1,,2]");
        TestUtil.assertMalformed("[[1,],2]");
    }

    @Test
    @DisplayName("array errors: bad elements")
    void badElements() {
        TestUtil.assertMalformed("[tru]");
        TestUtil.assertMalformed("[1,@]");
        TestUtil.assertMalformed("[01]");
        TestUtil.assertMalformed("[\"unterminated]");
        TestUtil.assertMalformed("[{\"a\"}]");
    }
}
