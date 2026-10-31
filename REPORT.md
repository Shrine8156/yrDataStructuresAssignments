# Written Report: JSON Parser

**Name:** Yeshas Reddi

**GitHub repository URL:** https://github.com/CPSC2108-Fall2026/cpsc-2108-fall-2026-json-parser-shrine8156

Answer in your own words, in complete sentences. A few sentences per part is enough.

---

## Question 1. Tracing the recursion (5 points)

Your parser is given this text:

```json
{"a": [1, {"b": true}], "c": {}}
```

**1a. (3)** List every call to `parseValue`, `parseObject`, and `parseArray` in the order the calls **begin**.
For each, give the part of the text it parses and what it returns (leave out `parseNumber`, `parseLiteral`, `parseString`).
Row 1 is started for you.

| # | Method | Part of the text | Returns |
|---|---|---|---|
| 1 | `parseValue` | the whole text | `HashMap` mapping `"a"` and `"c"` to their parsed values |
| 2 | `parseObject` | the whole text | `HashMap` mapping `"a"` and `"c"` to their parsed values |
| 3 | `parseValue` | `[1, {"b": true}]` | `ArrayList` containing `1` and `HashMap` `{b=true}` |
| 4 | `parseArray` | `[1, {"b": true}]` | `ArrayList` containing `1` and `HashMap` `{b=true}` |
| 5 | `parseValue` | `1` | `Integer` `1` |
| 6 | `parseValue` | `{"b": true}` | `HashMap` mapping `"b"` to `Boolean.TRUE` |
| 7 | `parseObject` | `{"b": true}` | `HashMap` mapping `"b"` to `Boolean.TRUE` |
| 8 | `parseValue` | `true` | `Boolean.TRUE` |
| 9 | `parseValue` | `{}` | empty `HashMap` `{}` |
| 10 | `parseObject` | `{}` | empty `HashMap` `{}` |

**1b. (1)** What is the maximum number of these calls in progress at the same time? Show how you counted.

The maximum number of calls in progress at the same time is 7. This maximum depth is reached when the parser is reading the boolean literal `true` inside the inner object. The active call stack at that moment is:
1. `parseValue` (entire input string)
2. `parseObject` (outer object)
3. `parseValue` (value for key `"a"`)
4. `parseArray` (the array `[1, {"b": true}]`)
5. `parseValue` (second element of the array: `{"b": true}`)
6. `parseObject` (inner object `{"b": true}`)
7. `parseValue` (value for key `"b"`: `true`)

**1c. (1)** What are the base cases of this recursion? Name at least three.

The base cases are the JSON values that do not contain further nested JSON structures and therefore do not make recursive calls to parse inner values. Examples include:
1. Strings (parsed directly by `parseString()`)
2. Numbers (integers, longs, and doubles parsed directly by `parseNumber()`)
3. Literals (`true`, `false`, and `null` parsed directly by `parseLiteral()`)
4. Empty objects `{}` and empty arrays `[]` also terminate recursion immediately without calling `parseValue()` on any contents.

## Question 2. HashMap (4 points)

**2a. (2)** In `LogDemo.failedLoginsPerUser`, why is looking up a user's count in a `HashMap` fast? Explain what
`hashCode` does and give the average Big-O. How would the lookup work, and what would its Big-O be, if the counts were stored in an `ArrayList` of (user, count) pairs?

Looking up a user's count in a `HashMap` is fast because it uses hash-based bucket indexing. When querying a user key, `user.hashCode()` computes an integer hash code that maps the string directly to an internal bucket index in constant time. The average time complexity for this lookup is $O(1)$. If the counts were stored in an `ArrayList` of (user, count) pairs, finding a user would require sequentially scanning the list from the beginning and calling `equals()` on each element until finding a match or reaching the end. This linear search would have an average and worst-case time complexity of $O(n)$, where $n$ is the number of distinct users.

**2b. (1)** When can a `HashMap` lookup become slow (O(n))?

A `HashMap` lookup degrades to $O(n)$ in the worst case when multiple keys produce hash collisions and map to the exact same bucket. When many keys collide into one bucket, the bucket becomes a linked list of entries that must be traversed one by one using `equals()`. This occurs when using a poor hash function or when adversarial inputs deliberately target the same hash bucket.

**2c. (1)** `parseObject` returns `Map<String, Object>`. What problem does `Object` as the value type cause for the code that uses the map?

Using `Object` as the value type strips away compile-time type information. Any code retrieving values from the map must manually cast each value to its expected type (such as `(String)`, `(List<Object>)`, or `(Map<String, Object>)`). If an unexpected JSON type is encountered at runtime, these manual casts fail with a `ClassCastException` instead of allowing the compiler to catch type mismatches at compile time.

## Question 3. HashSet in parseObject (4 points)

**3a. (2)** What does `set.add(key)` return, and how did you use that (or `contains`) in `parseObject`? Paste the lines of your code that detect a duplicate key.

`set.add(key)` returns a `boolean`: it returns `true` if the key was not already present and was added to the set, and `false` if the key already existed. In `parseObject`, we checked `seenKeys.contains(key)` before adding each key; if it was already present, we threw a `JsonParseException` pointing at the starting position of that duplicate key:

```java
int keyPos = pos;
String key = parseString();
if (seenKeys.contains(key)) {
    throw new JsonParseException("Duplicate key \"" + key + "\"", keyPos);
}
seenKeys.add(key);
```

**3b. (1)** `map.containsKey(key)` would also detect a duplicate. Give one reason you might still keep a separate `HashSet`, or one reason you would not.

One reason to keep a separate `HashSet` is separation of concerns: tracking syntax validity (duplicate detection during parsing) remains independent from the storage representation of values. If the parser is later changed to stream tokens or build custom domain objects without retaining a `HashMap`, duplicate checking remains intact. Conversely, a reason not to keep a separate `HashSet` is memory efficiency: allocating both a `HashMap` and a `HashSet` per JSON object duplicates bucket arrays and entry nodes, doubling allocation overhead when `map.containsKey(key)` already stores the exact same keys in memory.

**3c. (1)** Why might a security tool reject a log record with a duplicate key instead of letting the last value win?

Security tools reject duplicate keys to prevent parser differential attacks (JSON interoperability vulnerabilities). Different downstream parsers (such as web application firewalls, proxies, SIEMs, or backend databases) handle duplicate keys differently—some keep the first occurrence, while others keep the last occurrence. An attacker could exploit this discrepancy by crafting an event like `{"user": "alice", "user": "admin"}` to bypass filter rules while executing actions as admin. Rejecting duplicate keys ensures consistent and unambiguous interpretation across all tools.

## Question 4. Using the structures (5 points)

Run `./gradlew demo` and use the output of `LogDemo`.

**4a. (2)** `uniqueIps` returns a `HashSet`. Paste its output, then say how many IPs appear in `auth_events` in total
(use `auth.size()`) versus how many are unique. Why does a `HashSet` remove the duplicates without you writing a loop that compares them?

Output of `uniqueIps(auth)`:
`[10.0.4.35, 203.0.113.99, 198.51.100.23, 198.51.100.7, 10.0.7.19, 10.0.7.8, 192.0.2.14, 203.0.113.50, 10.0.4.22, 203.0.113.51, 10.0.4.21]`

There are 37 total authentication events in `auth_events` (`auth.size() == 37`), but only 11 unique IP addresses. A `HashSet` automatically removes duplicates because it computes the hash bucket for each string upon `add()`. If a string with the same hash code and `equals()` value is already present in that bucket, `add()` simply returns `false` without inserting a second copy. This deduplicates items in $O(1)$ average time per element without needing a nested comparison loop.

**4b. (2)** `eventsByUser` returns `Map<String, List<Object>>`. Which part is the `HashMap`, which is the `List`, and which one would you have to
`containsKey` before using for the first time? What is the type of the value for the key `"admin"`, and how many items does it have?

The outer structure mapping each username to its events is the `HashMap`, and the inner container holding the event records for each user is the `List` (`ArrayList`). You must call `containsKey` on the `HashMap` before adding an event for a user for the first time to verify whether an `ArrayList` has already been created for that user. The value for the key `"admin"` is an `ArrayList<Object>` (`List<Object>`), and it contains 11 items.

**4c. (1)** `flowsTouchingBlocklist` copies the blocklist `List` into a `HashSet` before looping over the flows. If it called `blocklist.contains(...)` instead,
how would the cost change as the blocklist and the number of flows grow?

If `blocklist.contains(...)` were called directly on an `ArrayList`, each lookup would require an $O(B)$ linear scan over the blocklist of size $B$. For $F$ flows (testing both source and destination), the total cost would be $O(F \times B)$. By copying the blocklist into a `HashSet` first in $O(B)$ time, every subsequent check is $O(1)$, bringing the total cost down to $O(B + F)$. As both the number of blocklist entries and flows grow into thousands or millions, $O(B + F)$ scales linearly, whereas $O(F \times B)$ grows quadratically.

## Question 5. Reflection (2 points)

Where did recursion, a `HashMap`, a `HashSet`, and a `List` each appear in this assignment? One sentence each. What was the hardest bug you hit, and how did you find it?

- **Recursion**: Recursion appeared in `JsonParser` where `parseValue`, `parseObject`, and `parseArray` call each other to parse arbitrarily nested objects and arrays.
- **HashMap**: A `HashMap` was used to represent JSON objects mapping string keys to values, and to count and group authentication logs by user in `LogDemo`.
- **HashSet**: A `HashSet` was used in `parseObject` to enforce the duplicate key rule, and in `LogDemo` to collect unique IP addresses and optimize blocklist lookups.
- **List**: An `ArrayList` was used to represent JSON arrays preserving sequence order, and to accumulate ordered event records for each user in `eventsByUser`.
- **Hardest bug**: The trickiest issue was properly handling delimiter whitespace and position tracking—specifically differentiating between valid empty containers (`[]`, `{}`) and malformed trailing commas (`[1,]`, `{"a":1,}`) without reading past the end of input; this was resolved by checking for closing delimiters before and after parsing comma separators and verifying cursor positions against test assertions.
