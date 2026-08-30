package com.jvalue.test;

import com.jvalue.Json;
import com.jvalue.JsonNumber;
import com.jvalue.JsonParseException;
import com.jvalue.JsonValue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.jvalue.test.TestRunner.*;

public final class JsonFileApiTest {

    public static void runAll() {
        runSuite("Json File API", () -> {
            runTest("Read valid UTF-8 object file", JsonFileApiTest::testReadValidUtf8Object);
            runTest("Read root primitives", JsonFileApiTest::testReadRootPrimitives);
            runTest("Read nested JSON", JsonFileApiTest::testReadNestedJson);
            runTest("Malformed JSON file rejected", JsonFileApiTest::testMalformedJsonFileRejected);
            runTest("Missing file throws IOException", JsonFileApiTest::testMissingFileThrowsIOException);
            runTest("Read rejects null path and charset", JsonFileApiTest::testReadRejectsNullPathAndCharset);
            runTest("Write rejects null value path and charset", JsonFileApiTest::testWriteRejectsNullValuePathAndCharset);
            runTest("Write compact file output", JsonFileApiTest::testWriteCompactFileOutput);
            runTest("Write pretty file output", JsonFileApiTest::testWritePrettyFileOutput);
            runTest("UTF-8 non-ASCII round trip", JsonFileApiTest::testUtf8NonAsciiRoundTrip);
            runTest("Escaping round trip", JsonFileApiTest::testEscapingRoundTrip);
            runTest("Written file reads back", JsonFileApiTest::testWrittenFileReadsBack);
            runTest("Invalid raw number write fails", JsonFileApiTest::testInvalidRawNumberWriteFails);
            runTest("Explicit charset overloads", JsonFileApiTest::testExplicitCharsetOverloads);
            runTest("File APIs preserve BOM parser policy", JsonFileApiTest::testFileApisPreserveBomParserPolicy);
        });
    }

    private static void testReadValidUtf8Object() {
        withTempDir(dir -> {
            Path file = dir.resolve("object.json");
            Files.writeString(file, "{\"name\":\"JValue\",\"ok\":true}", StandardCharsets.UTF_8);

            JsonValue value = Json.read(file);

            assertEquals("JValue", value.asObject().getString("name"));
            assertTrue(value.asObject().getBoolean("ok"));
        });
    }

    private static void testReadRootPrimitives() {
        withTempDir(dir -> {
            assertEquals(JsonValue.ofNull(), readWritten(dir, "null.json", "null"));
            assertEquals(JsonValue.of(true), readWritten(dir, "true.json", "true"));
            assertEquals(JsonValue.of(false), readWritten(dir, "false.json", "false"));
            assertEquals(JsonNumber.ofRaw("-0"), readWritten(dir, "number.json", "-0"));
            assertEquals(JsonValue.of("root"), readWritten(dir, "string.json", "\"root\""));
        });
    }

    private static void testReadNestedJson() {
        withTempDir(dir -> {
            Path file = dir.resolve("nested.json");
            String json = "{\"a\":[1,{\"b\":[true,null,\"x\"]}],\"n\":1E0}";
            Files.writeString(file, json, StandardCharsets.UTF_8);

            JsonValue value = Json.read(file);

            assertEquals(Json.parse(json), value);
            assertEquals("x", value.asObject().getArray("a").getObject(1).getArray("b").getString(2));
        });
    }

    private static void testMalformedJsonFileRejected() {
        withTempDir(dir -> {
            Path file = dir.resolve("bad.json");
            Files.writeString(file, "{\"missing\": [1, 2}", StandardCharsets.UTF_8);

            assertThrows(JsonParseException.class, () -> readUnchecked(file));
        });
    }

    private static void testMissingFileThrowsIOException() {
        withTempDir(dir -> {
            Path file = dir.resolve("missing.json");

            expectIOException(() -> Json.read(file));
        });
    }

    private static void testReadRejectsNullPathAndCharset() {
        assertThrows(NullPointerException.class, () -> readUnchecked(null));
        assertThrows(NullPointerException.class, () -> readUnchecked(null, StandardCharsets.UTF_8));
        withTempDir(dir -> assertThrows(NullPointerException.class,
                () -> readUnchecked(dir.resolve("x.json"), null)));
    }

    private static void testWriteRejectsNullValuePathAndCharset() {
        withTempDir(dir -> {
            Path file = dir.resolve("out.json");

            assertThrows(NullPointerException.class, () -> writeFileUnchecked(null, file));
            assertThrows(NullPointerException.class, () -> writeFileUnchecked(JsonValue.ofNull(), null));
            assertThrows(NullPointerException.class,
                    () -> writeFileUnchecked(JsonValue.ofNull(), file, null));
            assertThrows(NullPointerException.class, () -> writePrettyFileUnchecked(null, file));
            assertThrows(NullPointerException.class, () -> writePrettyFileUnchecked(JsonValue.ofNull(), null));
            assertThrows(NullPointerException.class,
                    () -> writePrettyFileUnchecked(JsonValue.ofNull(), file, null));
        });
    }

    private static void testWriteCompactFileOutput() {
        withTempDir(dir -> {
            Path file = dir.resolve("compact.json");
            JsonValue value = Json.parse("{\"z\":0,\"a\":[\"line\\n\",true]}");

            Json.writeFile(value, file);

            assertEquals("{\"z\":0,\"a\":[\"line\\n\",true]}",
                    Files.readString(file, StandardCharsets.UTF_8));
        });
    }

    private static void testWritePrettyFileOutput() {
        withTempDir(dir -> {
            Path file = dir.resolve("pretty.json");
            JsonValue value = Json.parse("{\"name\":\"JValue\",\"items\":[1,2]}");
            String expected = """
                    {
                      "name": "JValue",
                      "items": [
                        1,
                        2
                      ]
                    }""";

            Json.writePrettyFile(value, file);

            assertEquals(expected, Files.readString(file, StandardCharsets.UTF_8));
        });
    }

    private static void testUtf8NonAsciiRoundTrip() {
        withTempDir(dir -> {
            Path file = dir.resolve("utf8.json");
            JsonValue value = Json.parse("{\"text\":\"A\u00e9 \uD834\uDD1E \u65e5\u672c\"}");

            Json.writeFile(value, file);

            assertEquals(value, Json.read(file));
            assertEquals("{\"text\":\"A\u00e9 \uD834\uDD1E \u65e5\u672c\"}",
                    Files.readString(file, StandardCharsets.UTF_8));
        });
    }

    private static void testEscapingRoundTrip() {
        withTempDir(dir -> {
            Path file = dir.resolve("escaped.json");
            JsonValue value = Json.parse("{\"quote\":\"\\\"\",\"slash\":\"\\\\\",\"control\":\"\\u0000\\n\\t\"}");

            Json.writeFile(value, file);

            assertEquals(value, Json.read(file));
            assertEquals(Json.stringify(value), Files.readString(file, StandardCharsets.UTF_8));
        });
    }

    private static void testWrittenFileReadsBack() {
        withTempDir(dir -> {
            Path compact = dir.resolve("written.json");
            Path pretty = dir.resolve("written-pretty.json");
            JsonValue value = Json.parse("{\"root\":[{\"n\":1e+0},false,null,\"text\"]}");

            Json.writeFile(value, compact);
            Json.writePrettyFile(value, pretty);

            assertEquals(value, Json.read(compact));
            assertEquals(value, Json.read(pretty));
        });
    }

    private static void testInvalidRawNumberWriteFails() {
        withTempDir(dir -> {
            Path compact = dir.resolve("bad-number.json");
            Path pretty = dir.resolve("bad-number-pretty.json");
            JsonValue invalid = JsonNumber.ofRaw("01");

            assertThrows(IllegalArgumentException.class, () -> writeFileUnchecked(invalid, compact));
            assertThrows(IllegalArgumentException.class, () -> writePrettyFileUnchecked(invalid, pretty));
        });
    }

    private static void testExplicitCharsetOverloads() {
        withTempDir(dir -> {
            Path readFile = dir.resolve("latin1-read.json");
            Path writeFile = dir.resolve("latin1-write.json");
            Path prettyFile = dir.resolve("latin1-pretty.json");
            JsonValue value = Json.parse("{\"text\":\"caf\u00e9\"}");

            Files.writeString(readFile, "{\"text\":\"caf\u00e9\"}", StandardCharsets.ISO_8859_1);
            assertEquals(value, Json.read(readFile, StandardCharsets.ISO_8859_1));

            Json.writeFile(value, writeFile, StandardCharsets.ISO_8859_1);
            assertEquals(value, Json.read(writeFile, StandardCharsets.ISO_8859_1));

            Json.writePrettyFile(value, prettyFile, StandardCharsets.ISO_8859_1);
            assertEquals(value, Json.read(prettyFile, StandardCharsets.ISO_8859_1));
        });
    }

    private static void testFileApisPreserveBomParserPolicy() {
        withTempDir(dir -> {
            Path file = dir.resolve("bom.json");
            Files.writeString(file, "\uFEFF{}", StandardCharsets.UTF_8);

            assertThrows(JsonParseException.class, () -> readUnchecked(file));
        });
    }

    private static JsonValue readWritten(Path dir, String name, String json) throws IOException {
        Path file = dir.resolve(name);
        Files.writeString(file, json, StandardCharsets.UTF_8);
        return Json.read(file);
    }

    private static JsonValue readUnchecked(Path path) {
        try {
            return Json.read(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static JsonValue readUnchecked(Path path, Charset charset) {
        try {
            return Json.read(path, charset);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeFileUnchecked(JsonValue value, Path path) {
        try {
            Json.writeFile(value, path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeFileUnchecked(JsonValue value, Path path, Charset charset) {
        try {
            Json.writeFile(value, path, charset);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writePrettyFileUnchecked(JsonValue value, Path path) {
        try {
            Json.writePrettyFile(value, path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writePrettyFileUnchecked(JsonValue value, Path path, Charset charset) {
        try {
            Json.writePrettyFile(value, path, charset);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void withTempDir(TempDirAction action) {
        Path dir = null;
        try {
            dir = Files.createTempDirectory("jvalue-file-api-");
            action.run(dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (dir != null) {
                deleteRecursively(dir);
            }
        }
    }

    private static void deleteRecursively(Path path) {
        try (var paths = Files.walk(path)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void expectIOException(IoAction action) {
        try {
            action.run();
            throw new AssertionError("expected IOException but nothing was thrown");
        } catch (IOException e) {
            // expected
        }
    }

    @FunctionalInterface
    private interface TempDirAction {
        void run(Path dir) throws IOException;
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }

    private static final class UncheckedIOException extends RuntimeException {
        private UncheckedIOException(IOException cause) {
            super(cause);
        }
    }
}
