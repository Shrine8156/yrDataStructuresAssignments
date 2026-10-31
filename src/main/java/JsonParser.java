import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsonParser {

    protected final String text;
    protected int pos;

    protected JsonParser(String text) {
        this.text = text;
        this.pos = 0;
    }

    public static Object parse(String text) {
        return new JsonParser(text).parseDocument();
    }

    public static Object parseFile(String path) {
        try {
            return parse(Files.readString(Path.of(path)));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read file: " + path, e);
        }
    }

    protected Object parseDocument() {
        skipWhitespace();
        Object value = parseValue();
        skipWhitespace();
        if (!atEnd()) {
            throw error("Unexpected extra content after the JSON value");
        }
        return value;
    }

    protected boolean atEnd() {
        return pos >= text.length();
    }

    protected char peek() {
        if (atEnd()) {
            throw error("Unexpected end of input");
        }
        return text.charAt(pos);
    }

    protected char next() {
        char c = peek();
        pos++;
        return c;
    }

    protected void skipWhitespace() {
        while (!atEnd()) {
            char c = text.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else {
                return;
            }
        }
    }

    protected void expect(char expected) {
        if (atEnd()) {
            throw error("Expected '" + expected + "' but reached the end of the input");
        }
        char c = text.charAt(pos);
        if (c != expected) {
            throw error("Expected '" + expected + "' but found '" + c + "'");
        }
        pos++;
    }

    protected JsonParseException error(String message) {
        return new JsonParseException(message, pos);
    }

    protected static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    protected String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (atEnd()) {
                throw error("Unterminated string");
            }
            char c = text.charAt(pos);
            if (c == '"') {
                pos++;
                return sb.toString();
            }
            if (c == '\\') {
                int backslashPos = pos;
                pos++;
                if (atEnd()) {
                    throw error("Unterminated string");
                }
                char esc = text.charAt(pos);
                switch (esc) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case '/':
                        sb.append('/');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case 'r':
                        sb.append('\r');
                        break;
                    default:
                        throw new JsonParseException("Unsupported escape sequence '\\" + esc + "'", backslashPos);
                }
                pos++;
            } else if (c < 0x20) {
                throw error("Control character inside a string (use an escape such as backslash-n)");
            } else {
                sb.append(c);
                pos++;
            }
        }
    }

    protected Number parseNumber() {
        int start = pos;
        if (peek() == '-') {
            pos++;
        }
        if (atEnd() || !isDigit(text.charAt(pos))) {
            throw error("Expected a digit in number");
        }
        if (text.charAt(pos) == '0') {
            pos++;
            if (!atEnd() && isDigit(text.charAt(pos))) {
                throw error("Numbers cannot have leading zeros");
            }
        } else {
            while (!atEnd() && isDigit(text.charAt(pos))) {
                pos++;
            }
        }
        boolean hasDecimalPoint = false;
        if (!atEnd() && text.charAt(pos) == '.') {
            hasDecimalPoint = true;
            pos++;
            if (atEnd() || !isDigit(text.charAt(pos))) {
                throw error("Expected a digit after the decimal point");
            }
            while (!atEnd() && isDigit(text.charAt(pos))) {
                pos++;
            }
        }
        String token = text.substring(start, pos);
        if (hasDecimalPoint) {
            return Double.parseDouble(token);
        }
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException tooBigForInt) {
            try {
                return Long.parseLong(token);
            } catch (NumberFormatException tooBigForLong) {
                throw new JsonParseException("Integer is too large", start);
            }
        }
    }

    protected Object parseLiteral() {
        if (text.startsWith("true", pos)) {
            pos += 4;
            return Boolean.TRUE;
        }
        if (text.startsWith("false", pos)) {
            pos += 5;
            return Boolean.FALSE;
        }
        if (text.startsWith("null", pos)) {
            pos += 4;
            return null;
        }
        throw error("Invalid literal (expected true, false, or null)");
    }

    // TODO (README 4.3): parseValue
    protected Object parseValue() {
        skipWhitespace();
        char c = peek();
        if (c == '{') {
            return parseObject();
        } else if (c == '[') {
            return parseArray();
        } else if (c == '"') {
            return parseString();
        } else if (c == '-' || isDigit(c)) {
            return parseNumber();
        } else if (c == 't' || c == 'f' || c == 'n') {
            return parseLiteral();
        } else {
            throw error("Unexpected character '" + c + "'");
        }
    }

    // TODO (README 4.4): parseArray
    protected List<Object> parseArray() {
        expect('[');
        List<Object> list = new ArrayList<>();
        skipWhitespace();
        if (!atEnd() && peek() == ']') {
            pos++;
            return list;
        }
        while (true) {
            list.add(parseValue());
            skipWhitespace();
            if (atEnd()) {
                throw error("Expected ',' or ']' but reached the end of the input");
            }
            char c = peek();
            if (c == ',') {
                pos++;
                skipWhitespace();
                if (!atEnd() && peek() == ']') {
                    throw error("Trailing comma in array");
                }
            } else if (c == ']') {
                pos++;
                return list;
            } else {
                throw error("Expected ',' or ']' but found '" + c + "'");
            }
        }
    }

    // TODO (README 4.5): parseObject
    protected Map<String, Object> parseObject() {
        expect('{');
        Map<String, Object> map = new HashMap<>();
        Set<String> seenKeys = new HashSet<>();
        skipWhitespace();
        if (!atEnd() && peek() == '}') {
            pos++;
            return map;
        }
        while (true) {
            skipWhitespace();
            if (atEnd()) {
                throw error("Expected key string but reached end of input");
            }
            if (peek() != '"') {
                throw error("Expected string key but found '" + peek() + "'");
            }
            int keyPos = pos;
            String key = parseString();
            if (seenKeys.contains(key)) {
                throw new JsonParseException("Duplicate key \"" + key + "\"", keyPos);
            }
            seenKeys.add(key);

            skipWhitespace();
            expect(':');
            map.put(key, parseValue());
            skipWhitespace();

            if (atEnd()) {
                throw error("Expected ',' or '}' but reached end of input");
            }
            char c = peek();
            if (c == ',') {
                pos++;
                skipWhitespace();
                if (!atEnd() && peek() == '}') {
                    throw error("Trailing comma in object");
                }
            } else if (c == '}') {
                pos++;
                return map;
            } else {
                throw error("Expected ',' or '}' but found '" + c + "'");
            }
        }
    }
}
