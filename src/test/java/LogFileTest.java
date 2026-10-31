import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
class LogFileTest {

    private static Map<String, Object> root() {
        return TestUtil.asMap(JsonParser.parseFile("data/logs.json"));
    }

    private static List<Object> section(String name) {
        return TestUtil.asList(root().get(name));
    }

    private static Map<String, Object> item(String section, int index) {
        return TestUtil.asMap(section(section).get(index));
    }

    private static int[] counts(Object value) {
        int[] c = new int[3];
        walk(value, c);
        return c;
    }

    private static void walk(Object v, int[] c) {
        if (v instanceof Map) {
            c[1]++;
            for (Object x : TestUtil.asMap(v).values()) {
                walk(x, c);
            }
        } else if (v instanceof List) {
            c[2]++;
            for (Object x : TestUtil.asList(v)) {
                walk(x, c);
            }
        } else {
            c[0]++;
        }
    }

    @Test
    @DisplayName("logs.json: top-level keys")
    void topLevel() {
        Map<String, Object> r = root();
        assertEquals(6, r.size());
        assertEquals(new HashSet<>(List.of("source", "date", "blocklist", "auth_events", "network_flows", "alerts")),
                r.keySet());
        assertEquals("2026-09-01", r.get("date"));
    }

    @Test
    @DisplayName("logs.json: section sizes")
    void sizes() {
        assertEquals(3, section("blocklist").size());
        assertEquals(37, section("auth_events").size());
        assertEquals(34, section("network_flows").size());
        assertEquals(8, section("alerts").size());
    }

    @Test
    @DisplayName("logs.json: first auth event")
    void firstAuthEvent() {
        Map<String, Object> e = item("auth_events", 0);
        assertEquals(Integer.valueOf(1), e.get("id"));
        assertEquals("2026-09-01T08:01:11Z", e.get("time"));
        assertEquals("alice", e.get("user"));
        assertEquals("10.0.4.21", e.get("ip"));
        assertEquals(Boolean.TRUE, e.get("success"));
        assertTrue(e.containsKey("geo"));
        assertNull(e.get("geo"));
        assertTrue(!e.containsKey("mfa"));
    }

    @Test
    @DisplayName("logs.json: last auth event has nested geo data")
    void lastAuthEvent() {
        Map<String, Object> e = item("auth_events", 36);
        assertEquals("203.0.113.99", e.get("ip"));
        assertEquals(Boolean.FALSE, e.get("success"));
        Map<String, Object> geo = TestUtil.asMap(e.get("geo"));
        assertEquals("CN", geo.get("country"));
        assertEquals("Shenzhen", geo.get("city"));
    }

    @Test
    @DisplayName("logs.json: network flow with a list of flags")
    void firstFlow() {
        Map<String, Object> f = item("network_flows", 0);
        assertEquals("198.51.100.7", f.get("src"));
        assertEquals("10.0.1.10", f.get("dst"));
        assertEquals(Integer.valueOf(443), f.get("dst_port"));
        assertEquals("TCP", f.get("protocol"));
        assertEquals(TestUtil.list("SYN", "ACK", "PSH"), f.get("flags"));
        assertTrue(TestUtil.asList(item("network_flows", 23).get("flags")).isEmpty());
    }

    @Test
    @DisplayName("logs.json: large numbers and decimals in alerts")
    void alertNumbers() {
        Map<String, Object> a = item("alerts", 4);
        assertEquals("A-1005", a.get("id"));
        assertEquals(Integer.valueOf(52000000), TestUtil.asMap(a.get("details")).get("bytes"));
        assertEquals(Double.valueOf(412.5), TestUtil.asMap(item("alerts", 0).get("details")).get("window_seconds"));
        assertEquals(Double.valueOf(880.0), TestUtil.asMap(item("alerts", 1).get("details")).get("window_seconds"));
    }

    @Test
    @DisplayName("logs.json: escapes in alert notes")
    void alertEscapes() {
        assertEquals("Host \"db-01\" isolated", item("alerts", 3).get("note"));
        assertEquals("Backup account used; path C:\\backups\\nightly", item("alerts", 4).get("note"));
        assertEquals("", item("alerts", 5).get("note"));
    }

    @Test
    @DisplayName("logs.json: null values and empty containers")
    void nullsAndEmpties() {
        Map<String, Object> last = item("alerts", 7);
        assertTrue(last.containsKey("src_ip"));
        assertNull(last.get("src_ip"));
        assertTrue(TestUtil.asList(last.get("tags")).isEmpty());
        assertTrue(TestUtil.asMap(last.get("details")).isEmpty());
        assertNull(item("alerts", 0).get("resolved"));
        assertEquals(Boolean.TRUE, item("alerts", 2).get("resolved"));
    }

    @Test
    @DisplayName("logs.json: every value, object and array is there")
    void wholeFileCounts() {
        int[] c = counts(root());
        assertEquals(744, c[0], "values that are not objects or arrays");
        assertEquals(109, c[1], "objects");
        assertEquals(50, c[2], "arrays");
    }

    @Test
    @DisplayName("logs.json and logs.min.json parse to equal data")
    void minifiedMatches() throws IOException {
        Object pretty = JsonParser.parse(Files.readString(Path.of("data/logs.json")));
        Object compact = JsonParser.parse(Files.readString(Path.of("data/logs.min.json")));
        assertEquals(pretty, compact);
    }

    @Test
    @DisplayName("logs.json: every auth event has the same core keys")
    void coreKeys() {
        Set<String> core = Set.of("id", "time", "user", "ip", "action", "success", "method", "geo");
        for (Object e : section("auth_events")) {
            assertTrue(TestUtil.asMap(e).keySet().containsAll(core), "event is missing a key: " + e);
        }
    }
}
