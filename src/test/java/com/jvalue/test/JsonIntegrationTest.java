package com.jvalue.test;

import com.jvalue.Json;
import com.jvalue.JsonValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.jvalue.test.TestRunner.assertEquals;
import static com.jvalue.test.TestRunner.runSuite;
import static com.jvalue.test.TestRunner.runTest;

public class JsonIntegrationTest {

    public static void runAll() {
        runSuite("JsonIntegrationTest", () -> {
            runTest("Parse, Pointer, and Stringify", JsonIntegrationTest::testParsePointerStringify);
            runTest("File Read, Pointer, and Stringify", JsonIntegrationTest::testFileReadPointerStringify);
            runTest("File Write, Read, Pointer, and Stringify", JsonIntegrationTest::testFileWriteReadPointerStringify);
        });
    }

    private static void testParsePointerStringify() {
        String json = "{\"users\":[{\"name\":\"Ada\"},{\"name\":\"Grace\"}]}";
        JsonValue parsed = Json.parse(json);
        JsonValue grace = Json.pointer(parsed, "/users/1/name");
        String stringified = Json.stringify(grace);
        assertEquals("\"Grace\"", stringified);
    }

    private static void testFileReadPointerStringify() {
        try {
            Path tempFile = Files.createTempFile("jvalue-integration-", ".json");
            Files.writeString(tempFile, "{\"config\":{\"port\":8080}}", StandardCharsets.UTF_8);

            JsonValue doc = Json.read(tempFile);
            JsonValue port = Json.pointer(doc, "/config/port");
            String portString = Json.stringify(port);

            assertEquals("8080", portString);
            Files.deleteIfExists(tempFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void testFileWriteReadPointerStringify() {
        try {
            Path tempFile = Files.createTempFile("jvalue-integration-write-", ".json");

            String json = "{\"server\":{\"host\":\"localhost\"}}";
            JsonValue obj = Json.parse(json);

            Json.writeFile(obj, tempFile, StandardCharsets.UTF_8);

            JsonValue doc = Json.read(tempFile, StandardCharsets.UTF_8);
            JsonValue host = Json.pointer(doc, "/server/host");
            String hostString = Json.stringify(host);

            assertEquals("\"localhost\"", hostString);
            Files.deleteIfExists(tempFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
