package com.jvalue.test;

import com.jvalue.Json;
import com.jvalue.JsonValue;
import static com.jvalue.test.TestRunner.assertEquals;
import static com.jvalue.test.TestRunner.runSuite;
import static com.jvalue.test.TestRunner.runTest;

public class JsonRoundTripTest {

    public static void runAll() {
        runSuite("JsonRoundTripTest", () -> {
            runTest("Round-trip null", JsonRoundTripTest::testNull);
            runTest("Round-trip boolean", JsonRoundTripTest::testBoolean);
            runTest("Round-trip numbers", JsonRoundTripTest::testNumbers);
            runTest("Round-trip strings", JsonRoundTripTest::testStrings);
            runTest("Round-trip arrays", JsonRoundTripTest::testArrays);
            runTest("Round-trip objects", JsonRoundTripTest::testObjects);
            runTest("Round-trip deep nesting", JsonRoundTripTest::testDeepNesting);
        });
    }

    private static void assertRoundTrip(String originalJson) {
        JsonValue parsedOnce = Json.parse(originalJson);
        String stringifiedOnce = Json.stringify(parsedOnce);
        JsonValue parsedTwice = Json.parse(stringifiedOnce);
        String stringifiedTwice = Json.stringify(parsedTwice);

        assertEquals("Parsed trees should be equal", parsedOnce, parsedTwice);
        assertEquals("Stringified output should be equal", stringifiedOnce, stringifiedTwice);
    }

    private static void testNull() {
        assertRoundTrip("null");
        assertRoundTrip(" \n null \t ");
    }

    private static void testBoolean() {
        assertRoundTrip("true");
        assertRoundTrip("false");
    }

    private static void testNumbers() {
        assertRoundTrip("0");
        assertRoundTrip("-0");
        assertRoundTrip("1");
        assertRoundTrip("-1");
        assertRoundTrip("1.0");
        assertRoundTrip("1.5");
        assertRoundTrip("-1.5");
        assertRoundTrip("1e5");
        assertRoundTrip("1E5");
        assertRoundTrip("1.5e-5");
        assertRoundTrip("1.5E+5");
        assertRoundTrip("9007199254740992"); // 2^53
        assertRoundTrip("-9007199254740992");
        assertRoundTrip("1e-1000");
    }

    private static void testStrings() {
        assertRoundTrip("\"\"");
        assertRoundTrip("\"hello\"");
        assertRoundTrip("\"hello world\"");
        assertRoundTrip("\" \\\" \\\\ \\/ \\b \\f \\n \\r \\t \"");
        assertRoundTrip("\"\\u0000\\u001f\"");
        assertRoundTrip("\"𝄞\""); // surrogate pair
        assertRoundTrip("\"\\uD834\\uDD1E\""); // escaped surrogate pair
        assertRoundTrip("\"こんにちは\""); // unicode characters
    }

    private static void testArrays() {
        assertRoundTrip("[]");
        assertRoundTrip("[1,2,3]");
        assertRoundTrip("[\"a\",\"b\",\"c\"]");
        assertRoundTrip("[null,true,false]");
        assertRoundTrip("[[],[[]]]");
        assertRoundTrip("[1, \"two\", true, null, []]");
    }

    private static void testObjects() {
        assertRoundTrip("{}");
        assertRoundTrip("{\"a\":1}");
        assertRoundTrip("{\"a\":1,\"b\":2}");
        assertRoundTrip("{\"a\":1,\"a\":2}"); // Duplicate keys should resolve consistently (last wins)
        assertRoundTrip("{\"\":\"\"}"); // Empty string key
        assertRoundTrip("{\" \\\" \\\\ \":\"\"}"); // Escaped characters in key
        assertRoundTrip("{\"𝄞\":\"𝄞\"}"); // Surrogate pair in key
    }

    private static void testDeepNesting() {
        StringBuilder deepArray = new StringBuilder();
        for (int i = 0; i < 512; i++) deepArray.append("[");
        for (int i = 0; i < 512; i++) deepArray.append("]");
        assertRoundTrip(deepArray.toString());

        StringBuilder deepObject = new StringBuilder();
        for (int i = 0; i < 512; i++) deepObject.append("{\"a\":");
        deepObject.append("1");
        for (int i = 0; i < 512; i++) deepObject.append("}");
        assertRoundTrip(deepObject.toString());
    }
}
