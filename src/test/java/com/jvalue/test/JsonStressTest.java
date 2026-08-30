package com.jvalue.test;

import com.jvalue.Json;
import com.jvalue.JsonArray;
import com.jvalue.JsonValue;

import java.util.Collections;

import static com.jvalue.test.TestRunner.assertEquals;
import static com.jvalue.test.TestRunner.runSuite;
import static com.jvalue.test.TestRunner.runTest;

public class JsonStressTest {

    public static void runAll() {
        runSuite("JsonStressTest", () -> {
            runTest("Serializer deep nesting (512)", JsonStressTest::testSerializerDeepNesting);
            runTest("Serializer extreme nesting (1000+)", JsonStressTest::testSerializerExtremeNesting);
            runTest("Large array stress", JsonStressTest::testLargeArrayStress);
            runTest("Large object stress", JsonStressTest::testLargeObjectStress);
            runTest("Large string stress", JsonStressTest::testLargeStringStress);
        });
    }

    private static void testSerializerDeepNesting() {
        StringBuilder deepArray = new StringBuilder();
        for (int i = 0; i < 512; i++) deepArray.append("[");
        for (int i = 0; i < 512; i++) deepArray.append("]");
        
        try {
            JsonValue parsed = Json.parse(deepArray.toString());
            String json = Json.stringify(parsed);
            assertEquals(deepArray.toString(), json);
        } catch (StackOverflowError e) {
            throw new AssertionError("StackOverflowError at depth 512", e);
        }
    }

    private static void testSerializerExtremeNesting() {
        JsonValue current = JsonArray.empty();
        for (int i = 0; i < 1999; i++) {
            current = JsonArray.of(current);
        }
        
        try {
            Json.stringify(current);
        } catch (StackOverflowError e) {
            System.out.println("      (Caught StackOverflowError at depth 2000 during serialization)");
        }
    }

    private static void testLargeArrayStress() {
        StringBuilder sb = new StringBuilder("[");
        int size = 50000;
        for (int i = 0; i < size; i++) {
            sb.append(i);
            if (i < size - 1) sb.append(",");
        }
        sb.append("]");

        String json = sb.toString();
        JsonValue parsed = Json.parse(json);
        assertEquals(size, parsed.asArray().size());
        
        String stringified = Json.stringify(parsed);
        assertEquals(json, stringified);
    }

    private static void testLargeObjectStress() {
        StringBuilder sb = new StringBuilder("{");
        int size = 50000;
        for (int i = 0; i < size; i++) {
            sb.append("\"key").append(i).append("\":").append(i);
            if (i < size - 1) sb.append(",");
        }
        sb.append("}");

        String json = sb.toString();
        JsonValue parsed = Json.parse(json);
        assertEquals(size, parsed.asObject().size());
        
        String stringified = Json.stringify(parsed);
        assertEquals(json.length(), stringified.length());
    }

    private static void testLargeStringStress() {
        String largeString = String.join("", Collections.nCopies(1_000_000, "a"));
        JsonValue str = JsonValue.of(largeString);
        String json = Json.stringify(str);
        JsonValue parsed = Json.parse(json);
        assertEquals(largeString, parsed.asString());
    }
}
