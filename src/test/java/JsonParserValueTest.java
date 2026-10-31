import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(5)
class JsonParserValueTest {

    // ----- strings -----------------------------------------------------------------------

    @Test
    @DisplayName("string: simple")
    void simpleString() {
        assertEquals("hello", JsonParser.parse("\"hello\""));
    }

    @Test
    @DisplayName("string: empty")
    void emptyString() {
        assertEquals("", JsonParser.parse("\"\""));
    }

    @Test
    @DisplayName("string: spaces and punctuation are kept")
    void stringWithSpacesAndPunctuation() {
        assertEquals("a, b: [c] {d}", JsonParser.parse("\"a, b: [c] {d}\""));
    }

    @Test
    @DisplayName("string: escaped quote")
    void escapedQuote() {
        // JSON text:  "say \"hi\""
        assertEquals("say \"hi\"", JsonParser.parse("\"say \\\"hi\\\"\""));
    }

    @Test
    @DisplayName("string: escaped backslash")
    void escapedBackslash() {
        // JSON text:  "C:\\temp"
        assertEquals("C:\\temp", JsonParser.parse("\"C:\\\\temp\""));
    }

    @Test
    @DisplayName("string: newline, tab, carriage return, and slash escapes")
    void newlineTabEscapes() {
        // JSON text:  "a\nb\tc\rd\/e"
        assertEquals("a\nb\tc\rd/e", JsonParser.parse("\"a\\nb\\tc\\rd\\/e\""));
    }

    @Test
    @DisplayName("string: unsupported escapes are rejected")
    void unsupportedEscape() {
        // JSON text:  "\q"
        TestUtil.assertMalformed("\"\\q\"");
        // JSON text with a backslash followed by the letter u: Unicode escapes are not supported
        TestUtil.assertMalformed("\"\\" + "u0041\"");
    }

    @Test
    @DisplayName("string: unterminated")
    void unterminatedString() {
        TestUtil.assertMalformed("\"abc");
        TestUtil.assertMalformed("\"");
        // JSON text:  "abc\   (ends right after a backslash)
        TestUtil.assertMalformed("\"abc\\");
    }

    @Test
    @DisplayName("string: a raw newline inside a string is not allowed")
    void rawNewlineInString() {
        TestUtil.assertMalformed("\"line1\nline2\"");
    }

    // ----- numbers -----------------------------------------------------------------------

    @Test
    @DisplayName("number: whole numbers become Integer")
    void integers() {
        assertEquals(Integer.valueOf(0), JsonParser.parse("0"));
        assertEquals(Integer.valueOf(42), JsonParser.parse("42"));
        assertEquals(Integer.valueOf(-7), JsonParser.parse("-7"));
        assertEquals(Integer.valueOf(2147483647), JsonParser.parse("2147483647"));
    }

    @Test
    @DisplayName("number: too big for an int becomes Long")
    void bigIntegers() {
        assertEquals(Long.valueOf(2147483648L), JsonParser.parse("2147483648"));
        assertEquals(Long.valueOf(-3000000000L), JsonParser.parse("-3000000000"));
    }

    @Test
    @DisplayName("number: numbers with a decimal point become Double")
    void decimals() {
        assertEquals(Double.valueOf(3.14), JsonParser.parse("3.14"));
        assertEquals(Double.valueOf(-0.5), JsonParser.parse("-0.5"));
        assertEquals(Double.valueOf(0.0), JsonParser.parse("0.0"));
        assertEquals(Double.valueOf(10.0), JsonParser.parse("10.0"));
    }

    @Test
    @DisplayName("number: malformed numbers are rejected")
    void badNumbers() {
        TestUtil.assertMalformed("-");
        TestUtil.assertMalformed("--1");
        TestUtil.assertMalformed("+1");
        TestUtil.assertMalformed(".5");
        TestUtil.assertMalformed("1.");
        TestUtil.assertMalformed("01");
        TestUtil.assertMalformed("-01");
        TestUtil.assertMalformed("1.2.3");
        TestUtil.assertMalformed("1e5");   // exponents are not part of our subset
        TestUtil.assertMalformed("99999999999999999999");
    }

    // ----- true / false / null -----------------------------------------------------------

    @Test
    @DisplayName("literals: true, false, null")
    void literals() {
        assertEquals(Boolean.TRUE, JsonParser.parse("true"));
        assertEquals(Boolean.FALSE, JsonParser.parse("false"));
        assertNull(JsonParser.parse("null"));
    }

    @Test
    @DisplayName("literals: misspelled literals are rejected")
    void badLiterals() {
        TestUtil.assertMalformed("tru");
        TestUtil.assertMalformed("True");
        TestUtil.assertMalformed("nul");
        TestUtil.assertMalformed("NULL");
        TestUtil.assertMalformed("fals");
        TestUtil.assertMalformed("nulll");
    }

    // ----- whitespace, empty input, extra content ----------------------------------------

    @Test
    @DisplayName("whitespace around a value is ignored")
    void surroundingWhitespace() {
        assertEquals(Integer.valueOf(42), JsonParser.parse("  \n\t 42 \r\n  "));
        assertEquals("x", JsonParser.parse("\n\"x\"\n"));
    }

    @Test
    @DisplayName("empty or blank input is rejected")
    void emptyInput() {
        TestUtil.assertMalformed("");
        TestUtil.assertMalformed("   ");
        TestUtil.assertMalformed("\n\t");
    }

    @Test
    @DisplayName("extra content after the value is rejected")
    void extraContent() {
        TestUtil.assertMalformed("42 43");
        TestUtil.assertMalformed("true false");
        TestUtil.assertMalformed("\"a\" \"b\"");
        TestUtil.assertMalformed("null,");
    }

    @Test
    @DisplayName("unexpected characters are rejected")
    void unexpectedCharacters() {
        TestUtil.assertMalformed("@");
        TestUtil.assertMalformed("'single quotes'");
        TestUtil.assertMalformed("undefined");
    }

    // ----- error positions ---------------------------------------------------------------

    @Test
    @DisplayName("errors report the position of the problem")
    void errorPosition() {
        JsonParseException e1 = assertThrows(JsonParseException.class, () -> JsonParser.parse("  @"));
        assertEquals(2, e1.getPosition());
        assertTrue(e1.getMessage().contains("2"), "the message should mention the position");

        JsonParseException e2 = assertThrows(JsonParseException.class, () -> JsonParser.parse("42 x"));
        assertEquals(3, e2.getPosition());
    }

    // ----- files -------------------------------------------------------------------------

    @Test
    @DisplayName("parseFile: reads and parses a file")
    void parseFileWorks() {
        Object result = JsonParser.parseFile("data/scalar.json");
        assertEquals("just a string in a file", result);
    }

    @Test
    @DisplayName("parseFile: a missing file is an error")
    void parseFileMissing() {
        assertThrows(RuntimeException.class, () -> JsonParser.parseFile("data/does-not-exist.json"));
    }
}
