package com.jvalue;

import java.io.IOException;

import static com.jvalue.test.TestRunner.*;

public final class JsonSerializerTest {

    public static void runAll() {
        runSuite("JsonSerializer - Compact", () -> {
            runTest("Serialize primitives", JsonSerializerTest::testSerializePrimitives);
            runTest("Serialize number raw lexemes", JsonSerializerTest::testSerializeNumberRawLexemes);
            runTest("Serialize strings", JsonSerializerTest::testSerializeStrings);
            runTest("Serialize control characters", JsonSerializerTest::testSerializeControlCharacters);
            runTest("Serialize all control characters", JsonSerializerTest::testSerializeAllControlCharacters);
            runTest("Serialize Unicode", JsonSerializerTest::testSerializeUnicode);
            runTest("Reject lone surrogates", JsonSerializerTest::testRejectLoneSurrogates);
            runTest("Serialize arrays", JsonSerializerTest::testSerializeArrays);
            runTest("Serialize objects", JsonSerializerTest::testSerializeObjects);
            runTest("Serialize escaped object keys", JsonSerializerTest::testSerializeEscapedObjectKeys);
            runTest("Serialize preserves object insertion order", JsonSerializerTest::testSerializePreservesObjectInsertionOrder);
            runTest("Serialize deep nesting", JsonSerializerTest::testSerializeDeepNesting);
            runTest("Round-trip compact output", JsonSerializerTest::testRoundTripCompactOutput);
            runTest("JsonNumber.ofRaw rejects null and empty", JsonSerializerTest::testJsonNumberOfRawRejectsNullAndEmpty);
            runTest("Reject invalid raw numbers", JsonSerializerTest::testRejectInvalidRawNumbers);
            runTest("Write to Appendable", JsonSerializerTest::testWriteToAppendable);
        });

        runSuite("Json Public Serialization API", () -> {
            runTest("Json.write writes to StringBuilder", JsonSerializerTest::testJsonWriteStringBuilder);
            runTest("Json.stringify nested values", JsonSerializerTest::testJsonStringifyNestedValues);
            runTest("Json serialization rejects null inputs", JsonSerializerTest::testJsonSerializationRejectsNullInputs);
            runTest("Json.write propagates IOException", JsonSerializerTest::testJsonWritePropagatesIOException);
            runTest("Json.stringify and Json.write are identical", JsonSerializerTest::testJsonStringifyAndWriteAreIdentical);
        });

        runSuite("Json Pretty Serialization API", () -> {
            runTest("Pretty empty containers", JsonSerializerTest::testPrettyEmptyContainers);
            runTest("Pretty flat object", JsonSerializerTest::testPrettyFlatObject);
            runTest("Pretty flat array", JsonSerializerTest::testPrettyFlatArray);
            runTest("Pretty nested object", JsonSerializerTest::testPrettyNestedObject);
            runTest("Pretty nested array", JsonSerializerTest::testPrettyNestedArray);
            runTest("Pretty mixed object/array structure", JsonSerializerTest::testPrettyMixedObjectArrayStructure);
            runTest("Pretty escaped strings", JsonSerializerTest::testPrettyEscapedStrings);
            runTest("Pretty Unicode", JsonSerializerTest::testPrettyUnicode);
            runTest("Pretty nested indentation", JsonSerializerTest::testPrettyNestedIndentation);
            runTest("Json.writePretty writes to StringBuilder", JsonSerializerTest::testJsonWritePrettyStringBuilder);
            runTest("Json.writePretty propagates IOException", JsonSerializerTest::testJsonWritePrettyPropagatesIOException);
            runTest("Pretty serialization rejects null inputs", JsonSerializerTest::testPrettySerializationRejectsNullInputs);
            runTest("Json.stringifyPretty and Json.writePretty are identical",
                    JsonSerializerTest::testJsonStringifyPrettyAndWritePrettyAreIdentical);
            runTest("Compact and pretty preserve same values", JsonSerializerTest::testCompactAndPrettyPreserveSameValues);
        });
    }

    private static void testSerializePrimitives() {
        assertEquals("null", JsonSerializer.serialize(JsonNull.INSTANCE));
        assertEquals("true", JsonSerializer.serialize(JsonBoolean.TRUE));
        assertEquals("false", JsonSerializer.serialize(JsonBoolean.FALSE));
    }

    private static void testSerializeNumberRawLexemes() {
        String[] cases = {
            "0", "-0", "1", "1.0", "1.00", "1e0", "1E0", "1e+0",
            "0.1", "0.10", "9007199254740993", "1e308", "1e309"
        };
        for (String c : cases) {
            assertEquals("raw number " + c, c, JsonSerializer.serialize(JsonNumber.ofRaw(c)));
        }
    }

    private static void testSerializeStrings() {
        assertEquals("\"\"", JsonSerializer.serialize(JsonValue.of("")));
        assertEquals("\"hello\"", JsonSerializer.serialize(JsonValue.of("hello")));
        assertEquals("\"quote\\\"backslash\\\\slash/\"",
                JsonSerializer.serialize(JsonValue.of("quote\"backslash\\slash/")));
    }

    private static void testSerializeControlCharacters() {
        assertEquals("\"\\b\\f\\n\\r\\t\"", JsonSerializer.serialize(JsonValue.of("\b\f\n\r\t")));
        assertEquals("\"\\u0000\\u0001\\u001E\\u001F\"",
                JsonSerializer.serialize(JsonValue.of("\u0000\u0001\u001E\u001F")));
    }

    private static void testSerializeAllControlCharacters() {
        StringBuilder value = new StringBuilder();
        for (int i = 0; i <= 0x1F; i++) {
            value.append((char) i);
        }

        String expected = "\""
                + "\\u0000"
                + "\\u0001"
                + "\\u0002"
                + "\\u0003"
                + "\\u0004"
                + "\\u0005"
                + "\\u0006"
                + "\\u0007"
                + "\\b"
                + "\\t"
                + "\\n"
                + "\\u000B"
                + "\\f"
                + "\\r"
                + "\\u000E"
                + "\\u000F"
                + "\\u0010"
                + "\\u0011"
                + "\\u0012"
                + "\\u0013"
                + "\\u0014"
                + "\\u0015"
                + "\\u0016"
                + "\\u0017"
                + "\\u0018"
                + "\\u0019"
                + "\\u001A"
                + "\\u001B"
                + "\\u001C"
                + "\\u001D"
                + "\\u001E"
                + "\\u001F"
                + "\"";
        String serialized = Json.stringify(JsonValue.of(value.toString()));
        assertEquals(expected, serialized);
        assertEquals(JsonValue.of(value.toString()), Json.parse(serialized));
    }

    private static void testSerializeUnicode() {
        assertEquals("\"A\u00e9\"", JsonSerializer.serialize(JsonValue.of("A\u00e9")));
        assertEquals("\"music \uD834\uDD1E\"",
                JsonSerializer.serialize(JsonValue.of("music \uD834\uDD1E")));
        assertEquals(JsonValue.of("music \uD834\uDD1E"),
                Json.parse(JsonSerializer.serialize(JsonValue.of("music \uD834\uDD1E"))));
    }

    private static void testRejectLoneSurrogates() {
        assertThrows(IllegalArgumentException.class,
                () -> JsonSerializer.serialize(JsonValue.of("\uD800")));
        assertThrows(IllegalArgumentException.class,
                () -> JsonSerializer.serialize(JsonValue.of("\uDC00")));
        assertThrows(IllegalArgumentException.class,
                () -> JsonSerializer.serialize(JsonValue.of("\uD800x")));
    }

    private static void testSerializeArrays() {
        JsonArray empty = JsonArray.empty();
        assertEquals("[]", JsonSerializer.serialize(empty));

        JsonArray array = JsonArray.of(
                JsonValue.of(1),
                JsonValue.of("two"),
                JsonValue.of(true),
                JsonValue.ofNull(),
                JsonArray.of(JsonValue.of("nested"))
        );
        assertEquals("[1,\"two\",true,null,[\"nested\"]]", JsonSerializer.serialize(array));
    }

    private static void testSerializeObjects() {
        JsonObject empty = JsonObject.empty();
        assertEquals("{}", JsonSerializer.serialize(empty));

        JsonObject object = JsonObject.of(
                "name", JsonValue.of("JValue"),
                "ok", JsonValue.of(true)
        );
        assertEquals("{\"name\":\"JValue\",\"ok\":true}", JsonSerializer.serialize(object));
    }

    private static void testSerializeEscapedObjectKeys() {
        JsonObject object = JsonObject.of(
                "quote\"key", JsonValue.of(1),
                "line\nkey", JsonValue.of(2)
        );
        assertEquals("{\"quote\\\"key\":1,\"line\\nkey\":2}", JsonSerializer.serialize(object));
    }

    private static void testSerializePreservesObjectInsertionOrder() {
        JsonValue value = Json.parse("{\"z\":0,\"a\":1,\"m\":2}");
        assertEquals("{\"z\":0,\"a\":1,\"m\":2}", Json.stringify(value));
    }

    private static void testSerializeDeepNesting() {
        StringBuilder input = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            input.append('[');
        }
        input.append("\"leaf\"");
        for (int i = 0; i < 64; i++) {
            input.append(']');
        }

        JsonValue value = Json.parse(input.toString());
        assertEquals(input.toString(), Json.stringify(value));
        assertEquals(value, Json.parse(Json.stringify(value)));
    }

    private static void testRoundTripCompactOutput() {
        String[] inputs = {
            "null",
            "true",
            "false",
            "\"hello\"",
            "\"\\u0000\\u001F\"",
            "\"\\uD83D\\uDE00\"",
            "1E0",
            "-0",
            "[1,\"two\",true,null,{\"x\":[3.14]}]",
            "{\"a\":1,\"b\":\"two\",\"c\":[false,null]}"
        };
        for (String input : inputs) {
            JsonValue first = Json.parse(input);
            String serialized = JsonSerializer.serialize(first);
            JsonValue second = Json.parse(serialized);
            assertEquals("round-trip " + input, first, second);
        }
    }

    private static void testRejectInvalidRawNumbers() {
        String[] invalid = {
            "-", "-01", "00", "01", "+1", "1.", ".5", "1e", "1e+", "1e-", "1.e2",
            "1.0.0", "1 2", "NaN", "Infinity", "-Infinity"
        };
        for (String raw : invalid) {
            assertThrows(IllegalArgumentException.class,
                    () -> JsonSerializer.serialize(JsonNumber.ofRaw(raw)));
        }
    }

    private static void testJsonNumberOfRawRejectsNullAndEmpty() {
        assertThrows(NullPointerException.class, () -> JsonNumber.ofRaw(null));
        assertThrows(IllegalArgumentException.class, () -> JsonNumber.ofRaw(""));
    }

    private static void testWriteToAppendable() {
        StringBuilder out = new StringBuilder();
        try {
            JsonSerializer.write(JsonArray.of(JsonValue.of("x"), JsonValue.of(2)), out);
        } catch (IOException e) {
            throw new AssertionError("StringBuilder should not throw IOException", e);
        }
        assertEquals("[\"x\",2]", out.toString());
    }

    private static void testJsonWriteStringBuilder() {
        StringBuilder out = new StringBuilder();
        try {
            Json.write(JsonObject.of("message", JsonValue.of("hello\nworld")), out);
        } catch (IOException e) {
            throw new AssertionError("StringBuilder should not throw IOException", e);
        }
        assertEquals("{\"message\":\"hello\\nworld\"}", out.toString());
    }

    private static void testJsonStringifyNestedValues() {
        JsonValue value = JsonObject.of(
                "name", JsonValue.of("JValue"),
                "data", JsonArray.of(
                        JsonValue.of(-0.0),
                        JsonObject.of("enabled", JsonValue.of(true), "none", JsonValue.ofNull())
                )
        );

        assertEquals("{\"name\":\"JValue\",\"data\":[-0.0,{\"enabled\":true,\"none\":null}]}",
                Json.stringify(value));
        assertEquals(value, Json.parse(Json.stringify(value)));
    }

    private static void testJsonSerializationRejectsNullInputs() {
        assertThrows(NullPointerException.class, () -> Json.stringify(null));
        assertThrows(NullPointerException.class, () -> {
            try {
                Json.write(null, new StringBuilder());
            } catch (IOException e) {
                throw new AssertionError("StringBuilder should not throw IOException", e);
            }
        });
        assertThrows(NullPointerException.class, () -> {
            try {
                Json.write(JsonValue.ofNull(), null);
            } catch (IOException e) {
                throw new AssertionError("Null appendable should fail before IOException", e);
            }
        });
    }

    private static void testJsonWritePropagatesIOException() {
        IOException expected = new IOException("deliberate append failure");
        ThrowingAppendable out = new ThrowingAppendable(expected);

        try {
            Json.write(JsonValue.of("boom"), out);
            throw new AssertionError("expected IOException but nothing was thrown");
        } catch (IOException actual) {
            assertTrue("IOException should be propagated unchanged", actual == expected);
        }
    }

    private static void testJsonStringifyAndWriteAreIdentical() {
        JsonValue value = Json.parse("{\"a\":[1,\"two\",{\"b\":false}],\"n\":1e+0}");
        StringBuilder out = new StringBuilder();
        try {
            Json.write(value, out);
        } catch (IOException e) {
            throw new AssertionError("StringBuilder should not throw IOException", e);
        }
        assertEquals(Json.stringify(value), out.toString());
    }

    private static void testPrettyEmptyContainers() {
        assertEquals("{}", Json.stringifyPretty(JsonObject.empty()));
        assertEquals("[]", Json.stringifyPretty(JsonArray.empty()));
    }

    private static void testPrettyFlatObject() {
        JsonValue value = Json.parse("{\"name\":\"JValue\",\"version\":1}");
        String expected = """
                {
                  "name": "JValue",
                  "version": 1
                }""";
        assertEquals(expected, Json.stringifyPretty(value));
    }

    private static void testPrettyFlatArray() {
        JsonValue value = Json.parse("[1,\"two\",true,null]");
        String expected = """
                [
                  1,
                  "two",
                  true,
                  null
                ]""";
        assertEquals(expected, Json.stringifyPretty(value));
    }

    private static void testPrettyNestedObject() {
        JsonValue value = Json.parse("{\"outer\":{\"inner\":{\"value\":1}}}");
        String expected = """
                {
                  "outer": {
                    "inner": {
                      "value": 1
                    }
                  }
                }""";
        assertEquals(expected, Json.stringifyPretty(value));
    }

    private static void testPrettyNestedArray() {
        JsonValue value = Json.parse("[[1,2],[3,[4]]]");
        String expected = """
                [
                  [
                    1,
                    2
                  ],
                  [
                    3,
                    [
                      4
                    ]
                  ]
                ]""";
        assertEquals(expected, Json.stringifyPretty(value));
    }

    private static void testPrettyMixedObjectArrayStructure() {
        JsonValue value = Json.parse("{\"items\":[{\"id\":1,\"tags\":[\"a\",\"b\"]}],\"ok\":true}");
        String expected = """
                {
                  "items": [
                    {
                      "id": 1,
                      "tags": [
                        "a",
                        "b"
                      ]
                    }
                  ],
                  "ok": true
                }""";
        assertEquals(expected, Json.stringifyPretty(value));
    }

    private static void testPrettyEscapedStrings() {
        JsonValue value = JsonObject.of(
                "line\nkey",
                JsonValue.of("quote\" slash/ backslash\\ tab\t null\u0000")
        );
        String expected = """
                {
                  "line\\nkey": "quote\\" slash/ backslash\\\\ tab\\t null\\u0000"
                }""";
        assertEquals(expected, Json.stringifyPretty(value));
    }

    private static void testPrettyUnicode() {
        JsonValue value = JsonObject.of("text", JsonValue.of("A\u00e9 \uD834\uDD1E"));
        String expected = """
                {
                  "text": "A\u00e9 \uD834\uDD1E"
                }""";
        assertEquals(expected, Json.stringifyPretty(value));
    }

    private static void testPrettyNestedIndentation() {
        JsonValue value = Json.parse("{\"a\":[{\"b\":[{\"c\":[]}]}]}");
        String expected = """
                {
                  "a": [
                    {
                      "b": [
                        {
                          "c": []
                        }
                      ]
                    }
                  ]
                }""";
        assertEquals(expected, Json.stringifyPretty(value));
    }

    private static void testJsonWritePrettyStringBuilder() {
        StringBuilder out = new StringBuilder();
        try {
            Json.writePretty(JsonArray.of(JsonValue.of(1), JsonValue.of(2)), out);
        } catch (IOException e) {
            throw new AssertionError("StringBuilder should not throw IOException", e);
        }
        String expected = """
                [
                  1,
                  2
                ]""";
        assertEquals(expected, out.toString());
    }

    private static void testJsonWritePrettyPropagatesIOException() {
        IOException expected = new IOException("deliberate pretty append failure");
        ThrowingAppendable out = new ThrowingAppendable(expected);

        try {
            Json.writePretty(JsonArray.of(JsonValue.of(1)), out);
            throw new AssertionError("expected IOException but nothing was thrown");
        } catch (IOException actual) {
            assertTrue("IOException should be propagated unchanged", actual == expected);
        }
    }

    private static void testPrettySerializationRejectsNullInputs() {
        assertThrows(NullPointerException.class, () -> Json.stringifyPretty(null));
        assertThrows(NullPointerException.class, () -> {
            try {
                Json.writePretty(null, new StringBuilder());
            } catch (IOException e) {
                throw new AssertionError("StringBuilder should not throw IOException", e);
            }
        });
        assertThrows(NullPointerException.class, () -> {
            try {
                Json.writePretty(JsonValue.ofNull(), null);
            } catch (IOException e) {
                throw new AssertionError("Null appendable should fail before IOException", e);
            }
        });
    }

    private static void testJsonStringifyPrettyAndWritePrettyAreIdentical() {
        JsonValue value = Json.parse("{\"a\":[1,\"two\",{\"b\":false}],\"n\":1e+0}");
        StringBuilder out = new StringBuilder();
        try {
            Json.writePretty(value, out);
        } catch (IOException e) {
            throw new AssertionError("StringBuilder should not throw IOException", e);
        }
        assertEquals(Json.stringifyPretty(value), out.toString());
    }

    private static void testCompactAndPrettyPreserveSameValues() {
        String[] inputs = {
            "\"quote\\\"\\\\control\\u0000\"",
            "0",
            "-0",
            "1",
            "1.0",
            "1e0",
            "1E0",
            "1e+0",
            "999999999999999999999999999999999999",
            "{\"z\":0,\"a\":[-0,1E0,{\"key\\n\":\"value\\t\"}]}"
        };
        for (String input : inputs) {
            JsonValue original = Json.parse(input);
            JsonValue compact = Json.parse(Json.stringify(original));
            JsonValue pretty = Json.parse(Json.stringifyPretty(original));
            assertEquals("compact round-trip " + input, original, compact);
            assertEquals("pretty round-trip " + input, original, pretty);
        }
    }

    private static final class ThrowingAppendable implements Appendable {
        private final IOException exception;

        private ThrowingAppendable(IOException exception) {
            this.exception = exception;
        }

        @Override
        public Appendable append(CharSequence csq) throws IOException {
            throw exception;
        }

        @Override
        public Appendable append(CharSequence csq, int start, int end) throws IOException {
            throw exception;
        }

        @Override
        public Appendable append(char c) throws IOException {
            throw exception;
        }
    }
}
