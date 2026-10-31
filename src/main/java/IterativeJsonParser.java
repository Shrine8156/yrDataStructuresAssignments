import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IterativeJsonParser extends JsonParser {

    private static final class Frame {
        final boolean isObject;
        final Object container;
        final Set<String> seenKeys;
        String currentKey;

        Frame(boolean isObject, Object container, Set<String> seenKeys) {
            this.isObject = isObject;
            this.container = container;
            this.seenKeys = seenKeys;
            this.currentKey = null;
        }
    }

    protected IterativeJsonParser(String text) {
        super(text);
    }

    public static Object parse(String text) {
        return new IterativeJsonParser(text).parseDocument();
    }

    // TODO (README 8): bonus parseValue without recursion
    @Override
    @SuppressWarnings("unchecked")
    protected Object parseValue() {
        Deque<Frame> stack = new ArrayDeque<>();

        while (true) {
            skipWhitespace();
            char c = peek();
            Object completedValue = null;

            if (c == '{') {
                expect('{');
                skipWhitespace();
                if (!atEnd() && peek() == '}') {
                    pos++;
                    completedValue = new HashMap<String, Object>();
                } else {
                    Map<String, Object> map = new HashMap<>();
                    Set<String> seen = new HashSet<>();
                    Frame frame = new Frame(true, map, seen);
                    stack.push(frame);

                    skipWhitespace();
                    if (atEnd()) {
                        throw error("Expected key string but reached end of input");
                    }
                    if (peek() != '"') {
                        throw error("Expected string key but found '" + peek() + "'");
                    }
                    int keyPos = pos;
                    String key = parseString();
                    seen.add(key);
                    frame.currentKey = key;
                    skipWhitespace();
                    expect(':');
                    continue;
                }
            } else if (c == '[') {
                expect('[');
                skipWhitespace();
                if (!atEnd() && peek() == ']') {
                    pos++;
                    completedValue = new ArrayList<Object>();
                } else {
                    List<Object> list = new ArrayList<>();
                    Frame frame = new Frame(false, list, null);
                    stack.push(frame);
                    continue;
                }
            } else if (c == '"') {
                completedValue = parseString();
            } else if (c == '-' || isDigit(c)) {
                completedValue = parseNumber();
            } else if (c == 't' || c == 'f' || c == 'n') {
                completedValue = parseLiteral();
            } else {
                throw error("Unexpected character '" + c + "'");
            }

            if (stack.isEmpty()) {
                return completedValue;
            }

            while (true) {
                Frame top = stack.peek();
                if (!top.isObject) {
                    ((List<Object>) top.container).add(completedValue);
                    skipWhitespace();
                    if (atEnd()) {
                        throw error("Expected ',' or ']' but reached end of input");
                    }
                    char next = peek();
                    if (next == ',') {
                        pos++;
                        skipWhitespace();
                        if (!atEnd() && peek() == ']') {
                            throw error("Trailing comma in array");
                        }
                        break;
                    } else if (next == ']') {
                        pos++;
                        completedValue = top.container;
                        stack.pop();
                        if (stack.isEmpty()) {
                            return completedValue;
                        }
                    } else {
                        throw error("Expected ',' or ']' but found '" + next + "'");
                    }
                } else {
                    ((Map<String, Object>) top.container).put(top.currentKey, completedValue);
                    top.currentKey = null;
                    skipWhitespace();
                    if (atEnd()) {
                        throw error("Expected ',' or '}' but reached end of input");
                    }
                    char next = peek();
                    if (next == ',') {
                        pos++;
                        skipWhitespace();
                        if (!atEnd() && peek() == '}') {
                            throw error("Trailing comma in object");
                        }
                        if (peek() != '"') {
                            throw error("Expected string key but found '" + peek() + "'");
                        }
                        int keyPos = pos;
                        String key = parseString();
                        if (top.seenKeys.contains(key)) {
                            throw new JsonParseException("Duplicate key \"" + key + "\"", keyPos);
                        }
                        top.seenKeys.add(key);
                        top.currentKey = key;
                        skipWhitespace();
                        expect(':');
                        break;
                    } else if (next == '}') {
                        pos++;
                        completedValue = top.container;
                        stack.pop();
                        if (stack.isEmpty()) {
                            return completedValue;
                        }
                    } else {
                        throw error("Expected ',' or '}' but found '" + next + "'");
                    }
                }
            }
        }
    }
}
