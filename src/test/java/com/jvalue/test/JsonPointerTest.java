package com.jvalue.test;

import com.jvalue.Json;
import com.jvalue.JsonNull;
import com.jvalue.JsonPointer;
import com.jvalue.JsonValue;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.jvalue.test.TestRunner.*;

public final class JsonPointerTest {

    public static void runAll() {
        runSuite("JsonPointer - Token Parsing", () -> {
            runTest("Empty pointer has no tokens", JsonPointerTest::testEmptyPointer);
            runTest("Slash pointer has empty token", JsonPointerTest::testSlashPointer);
            runTest("Normal multi-token pointer", JsonPointerTest::testMultiTokenPointer);
            runTest("Decode tilde escape", JsonPointerTest::testDecodeTildeEscape);
            runTest("Decode slash escape", JsonPointerTest::testDecodeSlashEscape);
            runTest("Decode combined escapes", JsonPointerTest::testDecodeCombinedEscapes);
            runTest("Preserve consecutive empty tokens", JsonPointerTest::testConsecutiveEmptyTokens);
            runTest("Preserve spaces in tokens", JsonPointerTest::testSpaces);
            runTest("Preserve Unicode in tokens", JsonPointerTest::testUnicode);
            runTest("Reject pointer not beginning with slash", JsonPointerTest::testRejectNoLeadingSlash);
            runTest("Reject lone tilde", JsonPointerTest::testRejectLoneTilde);
            runTest("Reject trailing tilde", JsonPointerTest::testRejectTrailingTilde);
            runTest("Reject tilde two escape", JsonPointerTest::testRejectTildeTwo);
            runTest("Reject other invalid tilde escapes", JsonPointerTest::testRejectOtherInvalidEscapes);
            runTest("Reject null pointer", JsonPointerTest::testRejectNullPointer);
        });

        runSuite("JsonPointer - Tree Traversal", () -> {
            runTest("Empty pointer returns exact root object", JsonPointerTest::testEmptyPointerReturnsRootObject);
            runTest("Empty pointer returns root array", JsonPointerTest::testEmptyPointerReturnsRootArray);
            runTest("Empty pointer returns scalar roots", JsonPointerTest::testEmptyPointerReturnsScalarRoots);
            runTest("Object direct member lookup", JsonPointerTest::testObjectDirectMemberLookup);
            runTest("Object nested member lookup", JsonPointerTest::testObjectNestedMemberLookup);
            runTest("Object escaped key lookup", JsonPointerTest::testObjectEscapedKeyLookup);
            runTest("Object empty key lookup", JsonPointerTest::testObjectEmptyKeyLookup);
            runTest("Object slash and tilde keys", JsonPointerTest::testObjectSlashAndTildeKeys);
            runTest("Object numeric-looking keys", JsonPointerTest::testObjectNumericLookingKeys);
            runTest("Array first middle and last indexes", JsonPointerTest::testArrayIndexes);
            runTest("Nested array lookup", JsonPointerTest::testNestedArrayLookup);
            runTest("Mixed object array object traversal", JsonPointerTest::testMixedTraversal);
            runTest("Invalid array index forms are unresolved", JsonPointerTest::testInvalidArrayIndexForms);
            runTest("Scalar traversal is unresolved", JsonPointerTest::testScalarTraversalUnresolved);
            runTest("Missing target semantics", JsonPointerTest::testMissingTargetSemantics);
            runTest("Public Json.pointer APIs", JsonPointerTest::testPublicPointerApis);
            runTest("Parse pointer stringify integration", JsonPointerTest::testParsePointerStringifyIntegration);
            runTest("File read pointer integration", JsonPointerTest::testFileReadPointerIntegration);
            runTest("Deep object traversal", JsonPointerTest::testDeepObjectTraversal);
            runTest("Pointer query null handling", JsonPointerTest::testQueryNullHandling);
        });
    }

    private static void testEmptyPointer() {
        assertTokens("", List.of());
    }

    private static void testSlashPointer() {
        assertTokens("/", List.of(""));
    }

    private static void testMultiTokenPointer() {
        assertTokens("/a/b/c", List.of("a", "b", "c"));
    }

    private static void testDecodeTildeEscape() {
        assertTokens("/m~0n", List.of("m~n"));
    }

    private static void testDecodeSlashEscape() {
        assertTokens("/a~1b", List.of("a/b"));
    }

    private static void testDecodeCombinedEscapes() {
        assertTokens("/a~1b~0c", List.of("a/b~c"));
    }

    private static void testConsecutiveEmptyTokens() {
        assertTokens("/a//b", List.of("a", "", "b"));
        assertTokens("//", List.of("", ""));
    }

    private static void testSpaces() {
        assertTokens("/ a /b c/ ", List.of(" a ", "b c", " "));
    }

    private static void testUnicode() {
        assertTokens("/caf\u00e9/\uD834\uDD1E", List.of("caf\u00e9", "\uD834\uDD1E"));
    }

    private static void testRejectNoLeadingSlash() {
        assertThrows(IllegalArgumentException.class, () -> JsonPointer.compile("a/b"));
        assertThrows(IllegalArgumentException.class, () -> JsonPointer.compile(" "));
    }

    private static void testRejectLoneTilde() {
        assertThrows(IllegalArgumentException.class, () -> JsonPointer.compile("~"));
    }

    private static void testRejectTrailingTilde() {
        assertThrows(IllegalArgumentException.class, () -> JsonPointer.compile("/a~"));
    }

    private static void testRejectTildeTwo() {
        assertThrows(IllegalArgumentException.class, () -> JsonPointer.compile("/a~2b"));
    }

    private static void testRejectOtherInvalidEscapes() {
        assertThrows(IllegalArgumentException.class, () -> JsonPointer.compile("/a~xb"));
        assertThrows(IllegalArgumentException.class, () -> JsonPointer.compile("/a~~b"));
        assertThrows(IllegalArgumentException.class, () -> JsonPointer.compile("/a~/b"));
    }

    private static void testRejectNullPointer() {
        assertThrows(NullPointerException.class, () -> JsonPointer.compile(null));
    }

    private static void assertTokens(String pointer, List<String> expected) {
        JsonPointer compiled = JsonPointer.compile(pointer);
        assertEquals("tokens for " + pointer, expected, compiled.tokens());
        assertEquals("toString for " + pointer, pointer, compiled.toString());
    }

    private static void testEmptyPointerReturnsRootObject() {
        JsonValue root = Json.parse("{\"a\":1}");
        assertTrue(JsonPointer.compile("").query(root) == root);
        assertTrue(JsonPointer.compile("").queryOptional(root).orElseThrow() == root);
    }

    private static void testEmptyPointerReturnsRootArray() {
        JsonValue root = Json.parse("[1,2,3]");
        assertTrue(JsonPointer.compile("").query(root) == root);
    }

    private static void testEmptyPointerReturnsScalarRoots() {
        JsonValue string = Json.parse("\"root\"");
        JsonValue number = Json.parse("1");
        JsonValue bool = Json.parse("true");
        JsonValue nil = JsonNull.INSTANCE;

        assertTrue(JsonPointer.compile("").query(string) == string);
        assertTrue(JsonPointer.compile("").query(number) == number);
        assertTrue(JsonPointer.compile("").query(bool) == bool);
        assertTrue(JsonPointer.compile("").query(nil) == nil);
    }

    private static void testObjectDirectMemberLookup() {
        JsonValue root = Json.parse("{\"name\":\"JValue\",\"ok\":true}");
        assertEquals("JValue", JsonPointer.compile("/name").query(root).asString());
        assertTrue(JsonPointer.compile("/ok").query(root).asBoolean());
    }

    private static void testObjectNestedMemberLookup() {
        JsonValue root = Json.parse("{\"a\":{\"b\":{\"c\":3}}}");
        assertEquals(3, JsonPointer.compile("/a/b/c").query(root).asInt());
    }

    private static void testObjectEscapedKeyLookup() {
        JsonValue root = Json.parse("{\"a/b~c\":\"found\"}");
        assertEquals("found", JsonPointer.compile("/a~1b~0c").query(root).asString());
    }

    private static void testObjectEmptyKeyLookup() {
        JsonValue root = Json.parse("{\"\":\"empty\"}");
        assertEquals("empty", JsonPointer.compile("/").query(root).asString());
    }

    private static void testObjectSlashAndTildeKeys() {
        JsonValue root = Json.parse("{\"a/b\":\"slash\",\"m~n\":\"tilde\"}");
        assertEquals("slash", JsonPointer.compile("/a~1b").query(root).asString());
        assertEquals("tilde", JsonPointer.compile("/m~0n").query(root).asString());
    }

    private static void testObjectNumericLookingKeys() {
        JsonValue root = Json.parse("{\"0\":\"zero\",\"01\":\"leading\"}");
        assertEquals("zero", JsonPointer.compile("/0").query(root).asString());
        assertEquals("leading", JsonPointer.compile("/01").query(root).asString());
    }

    private static void testArrayIndexes() {
        JsonValue root = Json.parse("[\"zero\",\"one\",\"two\"]");
        assertEquals("zero", JsonPointer.compile("/0").query(root).asString());
        assertEquals("one", JsonPointer.compile("/1").query(root).asString());
        assertEquals("two", JsonPointer.compile("/2").query(root).asString());
    }

    private static void testNestedArrayLookup() {
        JsonValue root = Json.parse("[[\"a\"],[\"b\",[\"c\"]]]");
        assertEquals("c", JsonPointer.compile("/1/1/0").query(root).asString());
    }

    private static void testMixedTraversal() {
        JsonValue root = Json.parse("{\"items\":[{\"name\":\"first\"},{\"name\":\"second\"}]}");
        assertEquals("second", JsonPointer.compile("/items/1/name").query(root).asString());
    }

    private static void testInvalidArrayIndexForms() {
        JsonValue root = Json.parse("[\"zero\",\"one\"]");
        assertUnresolved(root, "/01");
        assertUnresolved(root, "/-1");
        assertUnresolved(root, "/+1");
        assertUnresolved(root, "/foo");
        assertUnresolved(root, "/-");
        assertUnresolved(root, "/");
        assertUnresolved(root, "/2147483648");
    }

    private static void testScalarTraversalUnresolved() {
        assertUnresolved(Json.parse("\"text\""), "/x");
        assertUnresolved(Json.parse("1"), "/x");
        assertUnresolved(Json.parse("true"), "/x");
        assertUnresolved(JsonValue.ofNull(), "/x");
    }

    private static void testMissingTargetSemantics() {
        JsonValue root = Json.parse("{\"a\":[1]}");
        assertUnresolved(root, "/missing");
        assertUnresolved(root, "/a/1");
        assertUnresolved(root, "/a/0/x");
    }

    private static void testPublicPointerApis() {
        JsonValue root = Json.parse("{\"a\":[\"x\",\"y\"]}");
        JsonPointer compiled = JsonPointer.compile("/a/1");

        assertEquals(compiled.query(root), Json.pointer(root, "/a/1"));
        assertEquals(compiled.queryOptional(root), Json.pointerOptional(root, "/a/1"));
        assertFalse(Json.pointerOptional(root, "/a/2").isPresent());

        assertThrows(NoSuchElementException.class, () -> Json.pointer(root, "/a/2"));
        assertThrows(IllegalArgumentException.class, () -> Json.pointer(root, "a/1"));
        assertThrows(IllegalArgumentException.class, () -> Json.pointerOptional(root, "/a~2"));
    }

    private static void testParsePointerStringifyIntegration() {
        JsonValue root = Json.parse("{\"result\":{\"values\":[10,{\"ok\":true}]}}");
        JsonValue result = Json.pointer(root, "/result/values/1");

        assertEquals("{\"ok\":true}", Json.stringify(result));
        assertEquals("""
                {
                  "ok": true
                }""", Json.stringifyPretty(result));
    }

    private static void testFileReadPointerIntegration() {
        Path dir = null;
        try {
            dir = Files.createTempDirectory("jvalue-pointer-");
            Path file = dir.resolve("data.json");
            Files.writeString(file, "{\"users\":[{\"name\":\"Ada\"}]}", StandardCharsets.UTF_8);

            JsonValue root = Json.read(file);
            assertEquals("Ada", Json.pointer(root, "/users/0/name").asString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (dir != null) {
                deleteRecursively(dir);
            }
        }
    }

    private static void testDeepObjectTraversal() {
        JsonValue root = Json.parse("{\"a\":{\"b\":{\"c\":{\"d\":{\"e\":{\"f\":\"leaf\"}}}}}}");
        assertEquals("leaf", Json.pointer(root, "/a/b/c/d/e/f").asString());
    }

    private static void testQueryNullHandling() {
        JsonPointer pointer = JsonPointer.compile("");

        assertThrows(NullPointerException.class, () -> pointer.query(null));
        assertThrows(NullPointerException.class, () -> pointer.queryOptional(null));
        assertThrows(NullPointerException.class, () -> Json.pointer(null, ""));
        assertThrows(NullPointerException.class, () -> Json.pointer(null, "not-a-pointer"));
        assertThrows(NullPointerException.class, () -> Json.pointer(JsonValue.ofNull(), null));
        assertThrows(NullPointerException.class, () -> Json.pointerOptional(null, ""));
        assertThrows(NullPointerException.class, () -> Json.pointerOptional(null, "not-a-pointer"));
        assertThrows(NullPointerException.class, () -> Json.pointerOptional(JsonValue.ofNull(), null));
    }

    private static void assertUnresolved(JsonValue root, String pointer) {
        JsonPointer compiled = JsonPointer.compile(pointer);
        assertThrows(NoSuchElementException.class, () -> compiled.query(root));
        assertFalse("optional should be empty for " + pointer, compiled.queryOptional(root).isPresent());
    }

    private static void deleteRecursively(Path path) {
        try (var paths = Files.walk(path)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
