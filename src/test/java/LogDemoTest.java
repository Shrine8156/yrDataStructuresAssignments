import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
class LogDemoTest {

    private static Map<String, Object> root() {
        return LogDemo.asMap(JsonParser.parseFile("data/logs.json"));
    }

    @Test
    @DisplayName("LogDemo: unique IPs (HashSet)")
    void uniqueIps() {
        Set<String> expected = new HashSet<>(List.of("10.0.4.21", "10.0.4.22", "10.0.4.35", "10.0.7.19", "10.0.7.8",
                "192.0.2.14", "198.51.100.23", "198.51.100.7", "203.0.113.50", "203.0.113.51", "203.0.113.99"));
        assertEquals(expected, LogDemo.uniqueIps(LogDemo.asList(root().get("auth_events"))));
    }

    @Test
    @DisplayName("LogDemo: failed logins per user (HashMap)")
    void failedLogins() {
        Map<String, Integer> expected = new HashMap<>();
        expected.put("admin", 10);
        expected.put("root", 4);
        expected.put("bob", 1);
        expected.put("carol", 2);
        expected.put("dave", 1);
        expected.put("alice", 1);
        assertEquals(expected, LogDemo.failedLoginsPerUser(LogDemo.asList(root().get("auth_events"))));
    }

    @Test
    @DisplayName("LogDemo: events grouped by user (HashMap of Lists)")
    void grouped() {
        Map<String, List<Object>> byUser = LogDemo.eventsByUser(LogDemo.asList(root().get("auth_events")));
        assertEquals(8, byUser.size());
        assertEquals(11, byUser.get("admin").size());
        assertEquals(5, byUser.get("alice").size());
    }

    @Test
    @DisplayName("LogDemo: flows touching the blocklist")
    void blocklist() {
        List<Object> hits = LogDemo.flowsTouchingBlocklist(LogDemo.asList(root().get("network_flows")),
                LogDemo.asList(root().get("blocklist")));
        assertEquals(19, hits.size());
    }
}
