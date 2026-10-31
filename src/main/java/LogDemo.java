import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LogDemo {

    @SuppressWarnings("unchecked")
    static Map<String, Object> asMap(Object o) {
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    static List<Object> asList(Object o) {
        return (List<Object>) o;
    }

    // TODO (README 5.2): uniqueIps
    static Set<String> uniqueIps(List<Object> authEvents) {
        Set<String> ips = new HashSet<>();
        for (Object event : authEvents) {
            Map<String, Object> map = asMap(event);
            Object ip = map.get("ip");
            if (ip instanceof String) {
                ips.add((String) ip);
            }
        }
        return ips;
    }

    // TODO (README 5.3): failedLoginsPerUser
    static Map<String, Integer> failedLoginsPerUser(List<Object> authEvents) {
        Map<String, Integer> counts = new HashMap<>();
        for (Object event : authEvents) {
            Map<String, Object> map = asMap(event);
            if (Boolean.FALSE.equals(map.get("success"))) {
                String user = (String) map.get("user");
                if (user != null) {
                    counts.put(user, counts.getOrDefault(user, 0) + 1);
                }
            }
        }
        return counts;
    }

    // TODO (README 5.4): eventsByUser
    static Map<String, List<Object>> eventsByUser(List<Object> authEvents) {
        Map<String, List<Object>> map = new HashMap<>();
        for (Object event : authEvents) {
            Map<String, Object> eventMap = asMap(event);
            String user = (String) eventMap.get("user");
            if (user != null) {
                if (!map.containsKey(user)) {
                    map.put(user, new ArrayList<>());
                }
                map.get(user).add(event);
            }
        }
        return map;
    }

    // PROVIDED (README 5.5): a finished example, do not change
    static List<Object> flowsTouchingBlocklist(List<Object> flows, List<Object> blocklist) {
        Set<Object> blocked = new HashSet<>(blocklist);
        List<Object> hits = new ArrayList<>();
        for (Object f : flows) {
            Map<String, Object> flow = asMap(f);
            if (blocked.contains(flow.get("src")) || blocked.contains(flow.get("dst"))) {
                hits.add(f);
            }
        }
        return hits;
    }

    public static void main(String[] args) {
        Map<String, Object> root = asMap(JsonParser.parseFile("data/logs.json"));
        List<Object> auth = asList(root.get("auth_events"));
        List<Object> flows = asList(root.get("network_flows"));
        List<Object> blocklist = asList(root.get("blocklist"));

        System.out.println("Auth events: " + auth.size());
        System.out.println("Unique source IPs: " + uniqueIps(auth).size() + " " + uniqueIps(auth));
        System.out.println("Failed logins per user: " + failedLoginsPerUser(auth));
        System.out.println("Users with events: " + eventsByUser(auth).keySet());
        System.out.println("Flows touching the blocklist: " + flowsTouchingBlocklist(flows, blocklist).size());
    }
}