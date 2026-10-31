# Build a JSON Parser (HashMap and HashSet)

**CPSC 2108 – Data Structures**

Security teams store their logs (logins, network traffic, alerts) as JSON. Every JSON **object** is a
**hash map** and every JSON **array** is a **list**. In this assignment you write the parser that
loads a JSON file into Java `HashMap`s, `HashSet`s, and `ArrayList`s, then you use those structures on
a set of security logs.

You will practice:

- `HashMap`: storing and looking up values by key
- `HashSet`: remembering what you have already seen
- `List` (`ArrayList`): ordered data and groups of records
- recursion: a JSON value can contain other JSON values

All instructions are in this README. In the code, every `TODO` names the README section to read.

---

## 1. Rules

| Rule | Details |
|---|---|
| **No JSON libraries or built-in JSON parsers.** | Do not use Jackson (`com.fasterxml`), Gson (`com.google.gson`), `org.json`, `javax.json` / `jakarta.json`, json-simple, Moshi, or a script engine (`javax.script`). Do not use regular expressions (`java.util.regex`) to parse. Read the text one character at a time with the provided helpers. |
| **No new dependencies.** | `build.gradle` may only depend on JUnit. |
| **Do not edit** `src/test/`, `data/`, or the provided methods in `JsonParser.java`. | You may add `private` helper methods. |
| **Use the required structures.** | Objects become `HashMap`, arrays become `ArrayList`, and `parseObject` must use a `HashSet` (section 4.5). |
| **GitHub with incremental commits.** | Commit and push each time something works. One "final version" commit is not acceptable. |

The autograder scans `src/main/java` and `build.gradle` for these libraries as a backstop. A person also reads your code.
Breaking the library rule earns no credit for the parser portion.

## 2. Repository layout

```
build.gradle, settings.gradle
data/
    example.json         tiny file to try first
    scalar.json          a file holding a single string
    logs.json            the security logs
    logs.min.json        the same logs on one line
src/main/java/
    JsonParseException.java   provided
    JsonParser.java           YOUR WORK: parseValue, parseArray, parseObject
    LogDemo.java              YOUR WORK: uniqueIps, failedLoginsPerUser, eventsByUser (section 5); one finished example
    IterativeJsonParser.java  bonus (section 8)
src/test/java/                tests (do not modify)
REPORT.md                     your written answers (section 7)
```

## 3. JSON in five minutes

JSON text is made of seven kinds of values:

| JSON | Example | Java type your parser produces |
|---|---|---|
| object | `{"user": "alice", "ok": true}` | `HashMap<String, Object>` |
| array | `[10, 20, 30]` | `ArrayList<Object>` |
| string | `"10.0.4.21"` | `String` |
| number, no decimal point | `443` | `Integer` (`Long` if too big for an int) |
| number with decimal point | `412.5` | `Double` |
| `true` / `false` | `true` | `Boolean` |
| `null` | `null` | Java `null` |

Objects and arrays can contain any value, including more objects and arrays:

```json
{
  "id": 7,
  "user": "admin",
  "geo": { "country": "RU", "city": "Moscow" },
  "tags": ["brute-force", "auth"],
  "resolved": null
}
```

This subset of JSON has: string escapes `\"` `\\` `\/` `\n` `\t` `\r` only (no unicode escapes); no exponents (`1e5`);
no leading zeros. Whitespace (space, tab, newline, carriage return) is allowed between any two tokens.
Try your parser first on `data/example.json`.

## 4. Writing the parser

### 4.1 How the parser reads text

`JsonParser` holds the whole text in `text` and a **cursor** `pos`, the index of the next character to read.
Every parse method follows one contract:

- **Before:** the cursor is on the first character of the thing being parsed (after any whitespace you skip).
- **After:** the cursor is on the first character **after** that thing.

Provided helpers (do not change them):

| Method | What it does |
|---|---|
| `atEnd()` | true when all characters are consumed |
| `peek()` | returns the current character without moving; throws `JsonParseException` at the end |
| `next()` | returns the current character and moves the cursor forward |
| `skipWhitespace()` | moves the cursor past spaces, tabs, newlines |
| `expect(c)` | consumes `c`, or throws `JsonParseException` if the current character is something else |
| `error(msg)` | builds a `JsonParseException` with the cursor position (use `throw error("...")`) |
| `isDigit(c)` | true for `'0'` to `'9'` |
| `parseString()` | cursor on opening `"`; returns the `String` |
| `parseNumber()` | cursor on `-` or a digit; returns `Integer`, `Long`, or `Double` |
| `parseLiteral()` | cursor on `t`, `f`, or `n`; returns `Boolean.TRUE`, `Boolean.FALSE`, or `null` |
| `parse(text)`, `parseFile(path)`, `parseDocument()` | entry points; they call your `parseValue()` |

Every error in malformed input must be a `JsonParseException`, never `StringIndexOutOfBoundsException` or
`NullPointerException`. Using `peek()` and `expect()` gives you this for free.

### 4.2 The recursion

`parseValue()` looks at one character and decides what kind of value follows. `parseObject()` and
`parseArray()` call `parseValue()` for each value inside them, so the three methods call each other.
The base cases are the values that contain nothing else: strings, numbers, `true`/`false`/`null`, `{}`, and `[]`.

### 4.3 `parseValue()`

1. Skip whitespace.
2. Look at the current character with `peek()` (do not consume it).
3. Call the method that matches:

   | Current character | Call |
   |---|---|
   | `{` | `parseObject()` |
   | `[` | `parseArray()` |
   | `"` | `parseString()` |
   | `-` or a digit | `parseNumber()` |
   | `t`, `f`, or `n` | `parseLiteral()` |

4. Any other character: `throw error("Unexpected character ...")`.

Finish this first. Strings, numbers, and literals then work, and `JsonParserValueTest` should pass.

### 4.4 `parseArray()`

Returns a new `ArrayList<Object>` with the elements in the order they appear.

1. `expect('[')`.
2. Skip whitespace. If the next character is `]`, consume it and return the empty list.
3. Otherwise loop: add `parseValue()` to the list, skip whitespace, then
   - `,`: consume it and go around again,
   - `]`: consume it and return the list,
   - anything else: throw an error.

A JSON `null` element is added as Java `null` (it still counts toward `size()`).
Malformed input to reject: missing comma, trailing comma (`[1,]`), missing `]`, `[,1]`.

### 4.5 `parseObject()`

Returns a new `HashMap<String, Object>` (the class `HashMap` exactly, not `TreeMap` or `LinkedHashMap`).

1. `expect('{')`.
2. Create the map **and** a `HashSet<String>` of keys seen so far.
3. Skip whitespace. If the next character is `}`, consume it and return the empty map.
4. Otherwise loop:
   1. Skip whitespace. The next character must be `"`, otherwise throw an error (keys are always strings).
   2. Read the key with `parseString()`.
   3. **Duplicate keys are an error.** If the key is already in the set, throw
      `new JsonParseException("Duplicate key ...", positionOfThatKey)`; otherwise add it to the set.
      (Remember where the key started: save `pos` before calling `parseString()`.)
   4. Skip whitespace, `expect(':')`, then `map.put(key, parseValue())`.
   5. Skip whitespace, then `,` (go around again), `}` (consume it and return the map), or an error.

A JSON `null` value is stored as a Java `null` value: the key is still in the map (`containsKey` is true).
The same key may appear in two different objects; the duplicate check applies within one object only.
Malformed input to reject: missing colon, missing value, missing comma, trailing comma, non-string key, missing `}`.

### 4.6 Suggested order and commits

1. `parseValue`: commit. 2. `parseArray`: commit. 3. `parseObject` without the duplicate check: commit.
4. Add the `HashSet` duplicate check: commit. 5. `LogDemo` (section 5), one method per commit.
6. Report and bonus: commit.

## 5. Using the parsed data: `LogDemo`

Once your parser works, you use it. `LogDemo.java` has four methods that each take the list of authentication events
(`auth_events`) or network flows from `data/logs.json`. You write three; the fourth is finished for you.

### 5.1 What the parsed file looks like

`JsonParser.parseFile("data/logs.json")` returns an `Object`. For `logs.json` it is a `HashMap` with these keys:
`source`, `date`, `blocklist` (list of IP strings), `auth_events`, `network_flows`, `alerts` (lists of objects).
`LogDemo` provides `asMap(...)` and `asList(...)` to cast for you:

```java
Map<String, Object> root = asMap(JsonParser.parseFile("data/logs.json"));
List<Object> events = asList(root.get("auth_events"));
Map<String, Object> first = asMap(events.get(0));
String user = (String) first.get("user");
```

Each auth event is a map with `id` (Integer), `time`, `user`, `ip`, `action`, `method` (Strings), `success` (Boolean),
`geo` (a map, or `null`), and sometimes `mfa` (Boolean). A missing key and a JSON `null` both give `null` from `get`;
`containsKey` tells them apart. Read a number whose type you do not know with `((Number) value).doubleValue()`.

Run `./gradlew demo` (runs `LogDemo.main`) and `./gradlew test --tests LogDemoTest` to check your work.

### 5.2 `uniqueIps` (HashSet)

Return a `HashSet<String>` containing every distinct `ip` value in the events. The tool: `Set.add`. Adding a value that is
already in the set changes nothing, so you do not write any code to compare or remove duplicates.

### 5.3 `failedLoginsPerUser` (HashMap, counting)

Return a `HashMap<String, Integer>` mapping each user to the number of events where `success` is `false`.
Users with no failures do not appear. The tools: `getOrDefault(key, 0)` and `put`. Pattern for "add one to a count":

```java
counts.put(user, counts.getOrDefault(user, 0) + 1);
```

### 5.4 `eventsByUser` (HashMap of Lists, grouping)

Return a `HashMap<String, List<Object>>` mapping each user to the list of that user's events, **in the order they appear**.
The first time you see a user there is no list yet: check with `containsKey`, create a new `ArrayList`, `put` it, then
`add` the event to the list you get back from the map.

### 5.5 `flowsTouchingBlocklist` (provided example)

Finished for you. Read it before you start: it copies the blocklist `List` into a `HashSet`, then uses `contains`
for each flow's source and destination. Question 4c of the report asks why.

## 6. Running the tests

```
./gradlew test                              all required tests
./gradlew test --tests JsonParserValueTest  one test class
./gradlew bonusTest                         bonus tests only
./gradlew demo                              run LogDemo
```

On Windows use `gradlew.bat`. Test classes: `JsonParserValueTest` (parseValue and the simple values),
`JsonParserArrayTest`, `JsonParserObjectTest`, `LogFileTest` (parses `data/logs.json`), `LogDemoTest` (your three `LogDemo` methods plus the example), `RulesTest`.
On day one most tests fail with "not implemented yet". Work through them in the order in section 4.6.
Every test has a time limit, so an infinite loop shows up as a failed test (usually the cursor was not advanced after `,`, `}`, or `]`).

## 7. Written report

Answer the questions in `REPORT.md`.

## 8. Bonus (+10): `IterativeJsonParser`

Recursion keeps track of "where am I inside all these nested objects and arrays?" on the call stack, which has
limited size. Rewrite `parseValue` in `IterativeJsonParser` with **no recursion**: keep your own stack
(for example a `java.util.ArrayDeque`) of the objects and arrays that are still open.

- Do not call `parseValue`, `parseObject`, or `parseArray` from inside it (the test checks the source).
- `parseString`, `parseNumber`, and `parseLiteral` may be used.
- Results and errors must match the recursive parser, including duplicate keys.
- Run with `./gradlew bonusTest`.

## 9. Submission

1. Push incrementally to GitHub as you work. Final code must be pushed to GitHub. Verify that your test cases pass locally. You repository must include:
   1. Completed code
   2. Completed REPORT.md
2. Screenshot of your test cases passing locally on your computer in CougarView.
3. A link to your GitHub repository in CougarView.