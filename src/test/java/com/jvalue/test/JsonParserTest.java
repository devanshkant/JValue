package com.jvalue.test;

import com.jvalue.Json;
import com.jvalue.JsonArray;
import com.jvalue.JsonBoolean;
import com.jvalue.JsonNull;
import com.jvalue.JsonObject;
import com.jvalue.JsonParseException;
import com.jvalue.JsonValue;

import static com.jvalue.test.TestRunner.*;

/**
 * Unit tests for the JValue recursive-descent JSON parser.
 *
 * <p>Covers: primitives, strings (escapes, Unicode, surrogates, control chars),
 * numbers (RFC 8259 grammar, rejection cases), arrays, objects, nesting,
 * error positions, trailing data, empty input, and round-trip checks.</p>
 */
public final class JsonParserTest {

    public static void runAll() {
        runSuite("JsonParser — Primitives", () -> {
            runTest("Parse null", JsonParserTest::testParseNull);
            runTest("Parse true", JsonParserTest::testParseTrue);
            runTest("Parse false", JsonParserTest::testParseFalse);
            runTest("Bare root values accepted", JsonParserTest::testBareRootValues);
        });

        runSuite("JsonParser — Strings", () -> {
            runTest("Simple string", JsonParserTest::testSimpleString);
            runTest("Empty string", JsonParserTest::testEmptyString);
            runTest("String with spaces", JsonParserTest::testStringWithSpaces);
            runTest("Single-character escapes", JsonParserTest::testSingleCharEscapes);
            runTest("Unicode escape basic", JsonParserTest::testUnicodeEscapeBasic);
            runTest("Unicode escape null char", JsonParserTest::testUnicodeEscapeNullChar);
            runTest("Surrogate pair", JsonParserTest::testSurrogatePair);
            runTest("Unescaped control chars rejected", JsonParserTest::testUnescapedControlCharsRejected);
            runTest("Invalid escape rejected", JsonParserTest::testInvalidEscapeRejected);
            runTest("Lone high surrogate rejected", JsonParserTest::testLoneHighSurrogateRejected);
            runTest("Lone low surrogate rejected", JsonParserTest::testLoneLowSurrogateRejected);
            runTest("Unterminated string rejected", JsonParserTest::testUnterminatedStringRejected);
            runTest("Incomplete unicode escape rejected", JsonParserTest::testIncompleteUnicodeEscapeRejected);
        });

        runSuite("JsonParser — Numbers", () -> {
            runTest("Integer", JsonParserTest::testInteger);
            runTest("Negative integer", JsonParserTest::testNegativeInteger);
            runTest("Zero", JsonParserTest::testZero);
            runTest("Negative zero", JsonParserTest::testNegativeZero);
            runTest("Decimal", JsonParserTest::testDecimal);
            runTest("Exponent", JsonParserTest::testExponent);
            runTest("Negative exponent", JsonParserTest::testNegativeExponent);
            runTest("Capital E exponent", JsonParserTest::testCapitalEExponent);
            runTest("Fraction and exponent", JsonParserTest::testFractionAndExponent);
            runTest("Raw preservation through parse", JsonParserTest::testRawPreservationThroughParse);
            runTest("Leading zeros rejected", JsonParserTest::testLeadingZerosRejected);
            runTest("Leading plus rejected", JsonParserTest::testLeadingPlusRejected);
            runTest("Bare decimal rejected", JsonParserTest::testBareDecimalRejected);
            runTest("Trailing decimal rejected", JsonParserTest::testTrailingDecimalRejected);
            runTest("Hex rejected", JsonParserTest::testHexRejected);
            runTest("NaN rejected", JsonParserTest::testNaNRejected);
            runTest("Infinity rejected", JsonParserTest::testInfinityRejected);
        });

        runSuite("JsonParser — Arrays", () -> {
            runTest("Empty array", JsonParserTest::testEmptyArray);
            runTest("Array with values", JsonParserTest::testArrayWithValues);
            runTest("Nested arrays", JsonParserTest::testNestedArrays);
            runTest("Missing comma in array rejected", JsonParserTest::testMissingCommaInArrayRejected);
            runTest("Trailing comma in array rejected", JsonParserTest::testTrailingCommaInArrayRejected);
            runTest("Unterminated array rejected", JsonParserTest::testUnterminatedArrayRejected);
        });

        runSuite("JsonParser — Objects", () -> {
            runTest("Empty object", JsonParserTest::testEmptyObject);
            runTest("Simple object", JsonParserTest::testSimpleObject);
            runTest("Nested object", JsonParserTest::testNestedObject);
            runTest("Duplicate keys last-value-wins", JsonParserTest::testDuplicateKeysLastValueWins);
            runTest("Missing colon rejected", JsonParserTest::testMissingColonRejected);
            runTest("Unquoted key rejected", JsonParserTest::testUnquotedKeyRejected);
            runTest("Trailing comma in object rejected", JsonParserTest::testTrailingCommaInObjectRejected);
            runTest("Unterminated object rejected", JsonParserTest::testUnterminatedObjectRejected);
        });

        runSuite("JsonParser — Whitespace & Structure", () -> {
            runTest("Leading and trailing whitespace", JsonParserTest::testLeadingTrailingWhitespace);
            runTest("Whitespace between tokens", JsonParserTest::testWhitespaceBetweenTokens);
            runTest("Empty input rejected", JsonParserTest::testEmptyInputRejected);
            runTest("Only whitespace rejected", JsonParserTest::testOnlyWhitespaceRejected);
            runTest("Trailing data rejected", JsonParserTest::testTrailingDataRejected);
            runTest("Null input rejected", JsonParserTest::testNullInputRejected);
        });

        runSuite("JsonParser — Error Positions", () -> {
            runTest("Error at start", JsonParserTest::testErrorAtStart);
            runTest("Error position after newlines", JsonParserTest::testErrorPositionAfterNewlines);
            runTest("Error in nested structure", JsonParserTest::testErrorInNestedStructure);
        });

        runSuite("JsonParser — Depth Limit", () -> {
            runTest("Depth 512 accepted", JsonParserTest::testDepth512Accepted);
            runTest("Depth 513 rejected", JsonParserTest::testDepth513Rejected);
        });

        runSuite("JsonParser — Complex Documents", () -> {
            runTest("Complex mixed document", JsonParserTest::testComplexMixedDocument);
            runTest("Round-trip simple values", JsonParserTest::testRoundTripSimpleValues);
        });
    }

    // === Primitives ===

    private static void testParseNull() {
        JsonValue v = Json.parse("null");
        assertTrue(v instanceof JsonNull);
        assertTrue(v.isNull());
    }

    private static void testParseTrue() {
        JsonValue v = Json.parse("true");
        assertTrue(v instanceof JsonBoolean);
        assertTrue(v.asBoolean());
    }

    private static void testParseFalse() {
        JsonValue v = Json.parse("false");
        assertTrue(v instanceof JsonBoolean);
        assertFalse(v.asBoolean());
    }

    private static void testBareRootValues() {
        // RFC 8259 allows any value as root
        assertTrue(Json.parse("null").isNull());
        assertTrue(Json.parse("true").isBoolean());
        assertTrue(Json.parse("false").isBoolean());
        assertTrue(Json.parse("42").isNumber());
        assertTrue(Json.parse("\"hello\"").isString());
        assertTrue(Json.parse("[]").isArray());
        assertTrue(Json.parse("{}").isObject());
    }

    // === Strings ===

    private static void testSimpleString() {
        JsonValue v = Json.parse("\"hello\"");
        assertEquals("hello", v.asString());
    }

    private static void testEmptyString() {
        JsonValue v = Json.parse("\"\"");
        assertEquals("", v.asString());
    }

    private static void testStringWithSpaces() {
        JsonValue v = Json.parse("\"hello world\"");
        assertEquals("hello world", v.asString());
    }

    private static void testSingleCharEscapes() {
        assertEquals("\"", Json.parse("\"\\\"\"").asString());
        assertEquals("\\", Json.parse("\"\\\\\"").asString());
        assertEquals("/", Json.parse("\"\\/\"").asString());
        assertEquals("\b", Json.parse("\"\\b\"").asString());
        assertEquals("\f", Json.parse("\"\\f\"").asString());
        assertEquals("\n", Json.parse("\"\\n\"").asString());
        assertEquals("\r", Json.parse("\"\\r\"").asString());
        assertEquals("\t", Json.parse("\"\\t\"").asString());
    }

    private static void testUnicodeEscapeBasic() {
        // \u0041 = 'A'
        assertEquals("A", Json.parse("\"\\u0041\"").asString());
        // \u00e9 = 'é'
        assertEquals("\u00e9", Json.parse("\"\\u00e9\"").asString());
        // Mixed with regular text
        assertEquals("Aé", Json.parse("\"\\u0041\\u00e9\"").asString());
    }

    private static void testUnicodeEscapeNullChar() {
        // \u0000 is valid JSON escape, produces Java string with null char
        String result = Json.parse("\"\\u0000\"").asString();
        assertEquals(1, result.length());
        assertEquals('\u0000', result.charAt(0));

        // \u001F is also valid as escape
        String result2 = Json.parse("\"\\u001F\"").asString();
        assertEquals(1, result2.length());
        assertEquals('\u001F', result2.charAt(0));
    }

    private static void testSurrogatePair() {
        // \uD83D\uDE00 = U+1F600 GRINNING FACE (😀)
        String result = Json.parse("\"\\uD83D\\uDE00\"").asString();
        assertEquals(2, result.length()); // surrogate pair in Java
        int codePoint = Character.toCodePoint(result.charAt(0), result.charAt(1));
        assertEquals(0x1F600, codePoint);
    }

    private static void testUnescapedControlCharsRejected() {
        // All literal control characters U+0000 to U+001F must be rejected
        for (int i = 0; i <= 0x1F; i++) {
            final String json = "\"" + (char) i + "\"";
            try {
                Json.parse(json);
                throw new AssertionError(
                    "Expected JsonParseException for unescaped control char U+"
                    + String.format("%04X", i) + " but parsing succeeded");
            } catch (JsonParseException e) {
                // expected
            }
        }
    }

    private static void testInvalidEscapeRejected() {
        assertThrows(JsonParseException.class, () -> Json.parse("\"\\a\""));
        assertThrows(JsonParseException.class, () -> Json.parse("\"\\x41\""));
        assertThrows(JsonParseException.class, () -> Json.parse("\"\\0\""));
        assertThrows(JsonParseException.class, () -> Json.parse("\"\\v\""));
    }

    private static void testLoneHighSurrogateRejected() {
        assertThrows(JsonParseException.class, () -> Json.parse("\"\\uD800\""));
        assertThrows(JsonParseException.class, () -> Json.parse("\"\\uDADA\""));
        assertThrows(JsonParseException.class, () -> Json.parse("\"\\uDBFF\""));
    }

    private static void testLoneLowSurrogateRejected() {
        assertThrows(JsonParseException.class, () -> Json.parse("\"\\uDC00\""));
        assertThrows(JsonParseException.class, () -> Json.parse("\"\\uDFFF\""));
        assertThrows(JsonParseException.class, () -> Json.parse("\"\\uDFAA\""));
    }

    private static void testUnterminatedStringRejected() {
        assertThrows(JsonParseException.class, () -> Json.parse("\"hello"));
        assertThrows(JsonParseException.class, () -> Json.parse("\""));
    }

    private static void testIncompleteUnicodeEscapeRejected() {
        assertThrows(JsonParseException.class, () -> Json.parse("\"\\u00\""));
        assertThrows(JsonParseException.class, () -> Json.parse("\"\\u\""));
        assertThrows(JsonParseException.class, () -> Json.parse("\"\\u00GG\""));
    }

    // === Numbers ===

    private static void testInteger() {
        JsonValue v = Json.parse("42");
        assertEquals(42, v.asInt());
        assertEquals("42", v.asJsonNumber().raw());
    }

    private static void testNegativeInteger() {
        assertEquals(-1, Json.parse("-1").asInt());
        assertEquals("-1", Json.parse("-1").asJsonNumber().raw());
    }

    private static void testZero() {
        assertEquals(0, Json.parse("0").asInt());
        assertEquals("0", Json.parse("0").asJsonNumber().raw());
    }

    private static void testNegativeZero() {
        assertEquals("-0", Json.parse("-0").asJsonNumber().raw());
    }

    private static void testDecimal() {
        assertEquals(3.14, Json.parse("3.14").asDouble());
        assertEquals("3.14", Json.parse("3.14").asJsonNumber().raw());
    }

    private static void testExponent() {
        assertEquals(100.0, Json.parse("1e2").asDouble());
        assertEquals("1e2", Json.parse("1e2").asJsonNumber().raw());
    }

    private static void testNegativeExponent() {
        assertEquals("1e-2", Json.parse("1e-2").asJsonNumber().raw());
    }

    private static void testCapitalEExponent() {
        assertEquals("1E2", Json.parse("1E2").asJsonNumber().raw());
    }

    private static void testFractionAndExponent() {
        assertEquals("1.5e10", Json.parse("1.5e10").asJsonNumber().raw());
    }

    private static void testRawPreservationThroughParse() {
        // Verify the parser preserves exact lexical form
        String[] cases = {"0", "-0", "1", "1.0", "1.00", "1e0", "1E0", "1e+0",
                          "0.1", "0.10", "3.14", "100", "1e308"};
        for (String c : cases) {
            JsonValue v = Json.parse(c);
            assertEquals("raw for " + c, c, v.asJsonNumber().raw());
        }
    }

    private static void testLeadingZerosRejected() {
        assertThrows(JsonParseException.class, () -> Json.parse("01"));
        assertThrows(JsonParseException.class, () -> Json.parse("007"));
        assertThrows(JsonParseException.class, () -> Json.parse("00"));
    }

    private static void testLeadingPlusRejected() {
        assertThrows(JsonParseException.class, () -> Json.parse("+1"));
    }

    private static void testBareDecimalRejected() {
        assertThrows(JsonParseException.class, () -> Json.parse(".5"));
    }

    private static void testTrailingDecimalRejected() {
        assertThrows(JsonParseException.class, () -> Json.parse("1."));
    }

    private static void testHexRejected() {
        assertThrows(JsonParseException.class, () -> Json.parse("0x1F"));
    }

    private static void testNaNRejected() {
        assertThrows(JsonParseException.class, () -> Json.parse("NaN"));
    }

    private static void testInfinityRejected() {
        assertThrows(JsonParseException.class, () -> Json.parse("Infinity"));
        assertThrows(JsonParseException.class, () -> Json.parse("-Infinity"));
    }

    // === Arrays ===

    private static void testEmptyArray() {
        JsonValue v = Json.parse("[]");
        assertTrue(v instanceof JsonArray);
        assertEquals(0, v.asArray().size());
    }

    private static void testArrayWithValues() {
        JsonArray arr = Json.parse("[1, \"two\", true, null, 3.14]").asArray();
        assertEquals(5, arr.size());
        assertEquals(1, arr.getInt(0));
        assertEquals("two", arr.getString(1));
        assertTrue(arr.getBoolean(2));
        assertTrue(arr.get(3).isNull());
        assertEquals(3.14, arr.get(4).asDouble());
    }

    private static void testNestedArrays() {
        JsonArray arr = Json.parse("[[1, 2], [3, [4, 5]]]").asArray();
        assertEquals(2, arr.size());
        JsonArray inner = arr.get(1).asArray();
        assertEquals(2, inner.size());
        assertEquals(4, inner.get(1).asArray().getInt(0));
    }

    private static void testMissingCommaInArrayRejected() {
        assertThrows(JsonParseException.class, () -> Json.parse("[1 2]"));
    }

    private static void testTrailingCommaInArrayRejected() {
        assertThrows(JsonParseException.class, () -> Json.parse("[1,]"));
    }

    private static void testUnterminatedArrayRejected() {
        assertThrows(JsonParseException.class, () -> Json.parse("[1, 2"));
        assertThrows(JsonParseException.class, () -> Json.parse("["));
    }

    // === Objects ===

    private static void testEmptyObject() {
        JsonValue v = Json.parse("{}");
        assertTrue(v instanceof JsonObject);
        assertEquals(0, v.asObject().size());
    }

    private static void testSimpleObject() {
        JsonObject obj = Json.parse("{\"name\": \"Alice\", \"age\": 30}").asObject();
        assertEquals(2, obj.size());
        assertEquals("Alice", obj.getString("name"));
        assertEquals(30, obj.getInt("age"));
    }

    private static void testNestedObject() {
        JsonObject obj = Json.parse("{\"a\": {\"b\": {\"c\": 1}}}").asObject();
        assertEquals(1, obj.get("a").asObject().get("b").asObject().getInt("c"));
    }

    private static void testDuplicateKeysLastValueWins() {
        JsonObject obj = Json.parse("{\"a\": 1, \"a\": 2}").asObject();
        assertEquals(1, obj.size());
        assertEquals(2, obj.getInt("a"));
    }

    private static void testMissingColonRejected() {
        assertThrows(JsonParseException.class, () -> Json.parse("{\"a\" 1}"));
    }

    private static void testUnquotedKeyRejected() {
        assertThrows(JsonParseException.class, () -> Json.parse("{a: 1}"));
    }

    private static void testTrailingCommaInObjectRejected() {
        assertThrows(JsonParseException.class, () -> Json.parse("{\"a\": 1,}"));
    }

    private static void testUnterminatedObjectRejected() {
        assertThrows(JsonParseException.class, () -> Json.parse("{\"a\": 1"));
        assertThrows(JsonParseException.class, () -> Json.parse("{"));
    }

    // === Whitespace & Structure ===

    private static void testLeadingTrailingWhitespace() {
        assertEquals(42, Json.parse("  42  ").asInt());
        assertEquals(42, Json.parse("\t42\n").asInt());
        assertEquals(42, Json.parse("\r\n42\r\n").asInt());
    }

    private static void testWhitespaceBetweenTokens() {
        JsonArray arr = Json.parse(" [ 1 , 2 , 3 ] ").asArray();
        assertEquals(3, arr.size());

        JsonObject obj = Json.parse(" { \"a\" : 1 } ").asObject();
        assertEquals(1, obj.getInt("a"));
    }

    private static void testEmptyInputRejected() {
        assertThrows(JsonParseException.class, () -> Json.parse(""));
    }

    private static void testOnlyWhitespaceRejected() {
        assertThrows(JsonParseException.class, () -> Json.parse("   "));
        assertThrows(JsonParseException.class, () -> Json.parse("\t\n\r "));
    }

    private static void testTrailingDataRejected() {
        assertThrows(JsonParseException.class, () -> Json.parse("123 456"));
        assertThrows(JsonParseException.class, () -> Json.parse("true false"));
        assertThrows(JsonParseException.class, () -> Json.parse("{}[]"));
        assertThrows(JsonParseException.class, () -> Json.parse("null null"));
    }

    private static void testNullInputRejected() {
        assertThrows(NullPointerException.class, () -> Json.parse(null));
    }

    // === Error Positions ===

    private static void testErrorAtStart() {
        try {
            Json.parse("xyz");
            throw new AssertionError("Expected JsonParseException");
        } catch (JsonParseException e) {
            assertEquals(1, e.line());
            assertEquals(1, e.column());
            assertEquals(0L, (long) e.offset());
        }
    }

    private static void testErrorPositionAfterNewlines() {
        // Error on line 3, column 5
        try {
            Json.parse("{\n  \"a\": 1,\n    xyz}");
            throw new AssertionError("Expected JsonParseException");
        } catch (JsonParseException e) {
            assertEquals(3, e.line());
            assertEquals(5, e.column());
        }
    }

    private static void testErrorInNestedStructure() {
        try {
            Json.parse("[1, 2, ]");
            throw new AssertionError("Expected JsonParseException");
        } catch (JsonParseException e) {
            // Should point to the ']' after the comma
            assertTrue("offset should be > 0", e.offset() > 0);
            assertTrue("message should be useful", e.getMessage().contains("Unexpected character"));
        }
    }

    // === Depth Limit ===

    private static void testDepth512Accepted() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 512; i++) sb.append('[');
        sb.append("null");
        for (int i = 0; i < 512; i++) sb.append(']');
        // Should parse successfully without exception
        JsonValue v = Json.parse(sb.toString());
        assertTrue(v instanceof JsonArray);
    }

    private static void testDepth513Rejected() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 513; i++) sb.append('[');
        sb.append("null");
        for (int i = 0; i < 513; i++) sb.append(']');
        assertThrows(JsonParseException.class, () -> Json.parse(sb.toString()));
    }

    // === Complex Documents ===

    private static void testComplexMixedDocument() {
        String json = """
            {
              "name": "JValue",
              "version": 1,
              "features": ["parsing", "serialization"],
              "config": {
                "maxDepth": 512,
                "strict": true
              },
              "metadata": null,
              "pi": 3.14159
            }
            """;
        JsonObject obj = Json.parse(json).asObject();
        assertEquals("JValue", obj.getString("name"));
        assertEquals(1, obj.getInt("version"));
        assertEquals(2, obj.get("features").asArray().size());
        assertEquals("parsing", obj.get("features").asArray().getString(0));
        assertEquals(512, obj.get("config").asObject().getInt("maxDepth"));
        assertTrue(obj.get("config").asObject().get("strict").asBoolean());
        assertTrue(obj.get("metadata").isNull());
        assertEquals(3.14159, obj.get("pi").asDouble());
    }

    private static void testRoundTripSimpleValues() {
        // Parse -> toString -> re-parse -> compare
        // Note: JsonString.toString() returns unquoted raw value,
        // so string round-trip via toString is not valid JSON.
        // String round-trip will be tested after the serializer (Phase 5).
        String[] inputs = {
            "null", "true", "false", "42", "3.14",
            "[]", "[1,2,3]", "{}"
        };
        for (String input : inputs) {
            JsonValue first = Json.parse(input);
            String serialized = first.toString();
            JsonValue second = Json.parse(serialized);
            assertEquals("Round-trip for " + input, first, second);
        }
    }
}
