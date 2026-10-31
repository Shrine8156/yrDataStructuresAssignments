import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Small helpers shared by the tests. DO NOT MODIFY any file in src/test.
 */
final class TestUtil {

    private TestUtil() {
    }

    /** map("a", 1, "b", null) builds a HashMap {a=1, b=null}. Null values are allowed. */
    static Map<String, Object> map(Object... keysAndValues) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            m.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return m;
    }

    /** list(1, "two", null) builds an ArrayList [1, two, null]. */
    static List<Object> list(Object... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> asMap(Object o) {
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    static List<Object> asList(Object o) {
        return (List<Object>) o;
    }

    /** Shortens very long inputs so failure messages stay readable. */
    private static String shorten(String json) {
        return json.length() <= 60 ? json : json.substring(0, 60) + "... (" + json.length() + " characters)";
    }

    /** Fails unless the recursive parser rejects this text with a JsonParseException. */
    static void assertMalformed(String json) {
        assertThrows(JsonParseException.class, () -> JsonParser.parse(json),
                "Expected a JsonParseException for this malformed input: " + shorten(json));
    }

    /** Fails unless the iterative (bonus) parser rejects this text with a JsonParseException. */
    static void assertMalformedIterative(String json) {
        assertThrows(JsonParseException.class, () -> IterativeJsonParser.parse(json),
                "Expected a JsonParseException for this malformed input: " + shorten(json));
    }
}
