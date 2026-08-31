import com.jvalue.*;
import java.nio.file.*;
import java.util.*;

/**
 * JValue Manual Demo — covers every feature + edge case.
 * Compile and run using the commands in demo/HOW_TO_RUN.md
 */
public class ManualDemo {

    static int passed = 0, failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   JValue Manual Feature Demo & Tester   ║");
        System.out.println("╚══════════════════════════════════════════╝\n");

        feature1_Parsing();
        feature2_ValueModel();
        feature3_Serialization();
        feature4_PrettySerialization();
        feature5_FileAPI();
        feature6_JsonPointer();
        feature7_ErrorHandling();
        feature8_RoundTrip();
        feature9_BuildFromJava();

        System.out.println("\n══════════════════════════════════════════");
        System.out.printf("  TOTAL: %d passed, %d failed%n", passed, failed);
        System.out.println("══════════════════════════════════════════");
        if (failed > 0) System.exit(1);
    }

    // ─────────────────────────────────────────────────────────────
    // FEATURE 1: PARSING (Json.parse)
    // ─────────────────────────────────────────────────────────────
    static void feature1_Parsing() {
        banner("FEATURE 1: Json.parse — RFC 8259 Parser");

        // Basic types
        check("Parse null",       Json.parse("null").isNull());
        check("Parse true",       Json.parse("true").asBoolean() == true);
        check("Parse false",      Json.parse("false").asBoolean() == false);
        check("Parse integer",    Json.parse("42").asInt() == 42);
        check("Parse negative",   Json.parse("-7").asInt() == -7);
        check("Parse decimal",    Json.parse("3.14").asDouble() == 3.14);
        check("Parse zero",       Json.parse("0").asInt() == 0);
        check("Parse -0",         Json.parse("-0").asJsonNumber().raw().equals("-0"));
        check("Parse 1e10",       Json.parse("1e10").asDouble() == 1e10);
        check("Parse 1E10",       Json.parse("1E10").asDouble() == 1E10);
        check("Parse 1.5e-3",     Json.parse("1.5e-3").asDouble() == 1.5e-3);
        check("Parse string",     Json.parse("\"hello\"").asString().equals("hello"));
        check("Parse empty str",  Json.parse("\"\"").asString().equals(""));

        // String escapes
        check("Escape \\\"",      Json.parse("\"a\\\"b\"").asString().equals("a\"b"));
        check("Escape \\\\",      Json.parse("\"a\\\\b\"").asString().equals("a\\b"));
        check("Escape \\n",       Json.parse("\"a\\nb\"").asString().equals("a\nb"));
        check("Escape \\t",       Json.parse("\"a\\tb\"").asString().equals("a\tb"));
        check("Escape \\r",       Json.parse("\"a\\rb\"").asString().equals("a\rb"));
        check("Escape \\b",       Json.parse("\"a\\bb\"").asString().equals("a\bb"));
        check("Escape \\f",       Json.parse("\"a\\fb\"").asString().equals("a\fb"));
        check("Escape \\/",       Json.parse("\"a\\/b\"").asString().equals("a/b"));
        check("Unicode \\u0041",  Json.parse("\"\\u0041\"").asString().equals("A"));
        check("Surrogate pair",   Json.parse("\"\\uD83D\\uDE00\"").asString().length() == 2);

        // Arrays and objects
        check("Empty array",  Json.parse("[]").asArray().isEmpty());
        check("Empty object", Json.parse("{}").asObject().isEmpty());
        check("Array size",   Json.parse("[1,2,3]").asArray().size() == 3);
        check("Object key",   Json.parse("{\"x\":1}").asObject().getInt("x") == 1);
        check("Nested",       Json.parse("{\"a\":{\"b\":99}}").asObject().getObject("a").getInt("b") == 99);
        check("Whitespace",   Json.parse("  { \"k\" : 1 }  ").asObject().getInt("k") == 1);

        // Duplicate keys — last-value-wins
        check("Dup key last-wins", Json.parse("{\"k\":1,\"k\":2}").asObject().getInt("k") == 2);

        // Parser rejects invalid
        expectThrow("Leading zero",   () -> Json.parse("01"));
        expectThrow("Leading plus",   () -> Json.parse("+1"));
        expectThrow("Bare decimal",   () -> Json.parse(".5"));
        expectThrow("Trailing comma", () -> Json.parse("[1,2,]"));
        expectThrow("Missing colon",  () -> Json.parse("{\"k\" 1}"));
        expectThrow("Unquoted key",   () -> Json.parse("{k:1}"));
        expectThrow("Empty input",    () -> Json.parse(""));
        expectThrow("Trailing data",  () -> Json.parse("1 2"));
        expectThrow("Lone high surr", () -> Json.parse("\"\\uD800\""));
        expectThrow("Lone low surr",  () -> Json.parse("\"\\uDC00\""));
        expectThrow("Control char",   () -> Json.parse("\"\u0001\""));
        expectThrow("NaN rejected",   () -> Json.parse("NaN"));
        expectThrow("Inf rejected",   () -> Json.parse("Infinity"));
        expectThrow("Null input",     () -> Json.parse(null));
    }

    // ─────────────────────────────────────────────────────────────
    // FEATURE 2: VALUE MODEL
    // ─────────────────────────────────────────────────────────────
    static void feature2_ValueModel() {
        banner("FEATURE 2: Value Model — JsonObject / JsonArray / JsonNumber / etc.");

        // Type checks
        JsonValue obj  = Json.parse("{\"a\":1}");
        JsonValue arr  = Json.parse("[1]");
        JsonValue str  = Json.parse("\"hi\"");
        JsonValue num  = Json.parse("42");
        JsonValue bool = Json.parse("true");
        JsonValue nul  = Json.parse("null");

        check("isObject",  obj.isObject()  && !obj.isArray());
        check("isArray",   arr.isArray()   && !arr.isObject());
        check("isString",  str.isString());
        check("isNumber",  num.isNumber());
        check("isBoolean", bool.isBoolean());
        check("isNull",    nul.isNull());

        // JsonObject API
        JsonObject o = Json.parse("{\"name\":\"Ada\",\"age\":36,\"pi\":3.14,\"ok\":true}").asObject();
        check("obj.getString",  o.getString("name").equals("Ada"));
        check("obj.getInt",     o.getInt("age") == 36);
        check("obj.getDouble",  o.getDouble("pi") == 3.14);
        check("obj.getBoolean", o.getBoolean("ok") == true);
        check("obj.has true",   o.has("name"));
        check("obj.has false",  !o.has("missing"));
        check("obj.size",       o.size() == 4);
        check("obj.keys",       o.keys().contains("name"));
        check("obj.get null",   o.get("missing") == null);
        check("obj.isEmpty false", !o.isEmpty());
        check("obj empty",      JsonObject.empty().isEmpty());

        // JsonArray API
        JsonArray a = Json.parse("[10, \"two\", true, null]").asArray();
        check("arr.getInt",     a.getInt(0) == 10);
        check("arr.getString",  a.getString(1).equals("two"));
        check("arr.getBoolean", a.getBoolean(2) == true);
        check("arr[3].isNull",  a.get(3).isNull());
        check("arr.size",       a.size() == 4);
        check("arr empty",      JsonArray.empty().isEmpty());

        // JsonNumber precision
        JsonNumber n1 = Json.parse("1").asJsonNumber();
        JsonNumber n2 = Json.parse("1.0").asJsonNumber();
        JsonNumber n3 = Json.parse("1e0").asJsonNumber();
        check("raw '1'",   n1.raw().equals("1"));
        check("raw '1.0'", n2.raw().equals("1.0"));
        check("raw '1e0'", n3.raw().equals("1e0"));
        check("1 != 1.0 (lexical)", !n1.equals(n2));  // intentional — raw equality
        check("BigDecimal same", n1.asBigDecimal().compareTo(n2.asBigDecimal()) == 0);

        // Factories
        check("JsonValue.of(int)",    Json.parse(Json.stringify(JsonValue.of(5))).asInt() == 5);
        check("JsonValue.of(long)",   Json.parse(Json.stringify(JsonValue.of(5L))).asLong() == 5L);
        check("JsonValue.of(double)", Json.parse(Json.stringify(JsonValue.of(1.5))).asDouble() == 1.5);
        check("JsonValue.of(String)", Json.parse(Json.stringify(JsonValue.of("hi"))).asString().equals("hi"));
        check("JsonValue.of(bool)",   Json.parse(Json.stringify(JsonValue.of(true))).asBoolean());
        check("JsonValue.ofNull",     Json.parse(Json.stringify(JsonValue.ofNull())).isNull());

        // JsonArray.of factory
        JsonArray built = JsonArray.of(JsonValue.of(1), JsonValue.of("x"), JsonValue.of(true));
        check("JsonArray.of size", built.size() == 3);

        // JsonObject.of factory
        JsonObject built2 = JsonObject.of("k1", JsonValue.of("v1"), "k2", JsonValue.of(99));
        check("JsonObject.of getString", built2.getString("k1").equals("v1"));
        check("JsonObject.of getInt",    built2.getInt("k2") == 99);

        // Iteration
        int count = 0;
        for (var entry : Json.parse("{\"a\":1,\"b\":2}").asObject()) count++;
        check("Object iteration", count == 2);

        // Depth limit
        expectThrow("Depth > 512", () -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 513; i++) sb.append("[");
            for (int i = 0; i < 513; i++) sb.append("]");
            Json.parse(sb.toString());
        });
        check("Depth == 512 ok", Json.parse(
            "[".repeat(512) + "1" + "]".repeat(512)).asArray().size() == 1);
    }

    // ─────────────────────────────────────────────────────────────
    // FEATURE 3: COMPACT SERIALIZATION
    // ─────────────────────────────────────────────────────────────
    static void feature3_Serialization() throws Exception {
        banner("FEATURE 3: Json.stringify — Compact Serialization");

        check("null",    Json.stringify(JsonValue.ofNull()).equals("null"));
        check("true",    Json.stringify(JsonValue.of(true)).equals("true"));
        check("false",   Json.stringify(JsonValue.of(false)).equals("false"));
        check("int",     Json.stringify(JsonValue.of(42)).equals("42"));
        check("double",  Json.stringify(JsonValue.of(3.14)).equals("3.14"));
        check("string",  Json.stringify(JsonValue.of("hi")).equals("\"hi\""));
        check("empty []", Json.stringify(JsonArray.empty()).equals("[]"));
        check("empty {}", Json.stringify(JsonObject.empty()).equals("{}"));

        // Escaping in output
        JsonValue withQuote = Json.parse("\"a\\\"b\"");
        check("Output escapes quote", Json.stringify(withQuote).contains("\\\""));

        JsonValue withNewline = Json.parse("\"a\\nb\"");
        check("Output escapes newline", Json.stringify(withNewline).contains("\\n"));

        // Raw lexeme preservation
        check("-0 preserved",  Json.stringify(Json.parse("-0")).equals("-0"));
        check("1e10 preserved", Json.stringify(Json.parse("1e10")).equals("1e10"));
        check("1.0 preserved",  Json.stringify(Json.parse("1.0")).equals("1.0"));

        // Insertion order preserved
        String objStr = Json.stringify(Json.parse("{\"b\":2,\"a\":1,\"c\":3}"));
        check("Insertion order b,a,c", objStr.indexOf("\"b\"") < objStr.indexOf("\"a\""));

        // Appendable output
        StringBuilder sb = new StringBuilder();
        Json.write(JsonValue.of("test"), sb);
        check("Write to Appendable", sb.toString().equals("\"test\""));

        // Null rejection
        expectThrow("stringify null", () -> Json.stringify(null));
        expectThrow("write null val", () -> { try { Json.write(null, new StringBuilder()); } catch(Exception e) { throw new RuntimeException(e); }});
    }

    // ─────────────────────────────────────────────────────────────
    // FEATURE 4: PRETTY SERIALIZATION
    // ─────────────────────────────────────────────────────────────
    static void feature4_PrettySerialization() throws Exception {
        banner("FEATURE 4: Json.stringifyPretty — Pretty Serialization");

        String pretty = Json.stringifyPretty(Json.parse("{\"a\":1,\"b\":[2,3]}"));
        check("Pretty has newlines",    pretty.contains("\n"));
        check("Pretty has 2-space",     pretty.contains("  "));
        check("Pretty no trail newline", !pretty.endsWith("\n"));
        System.out.println("  [Sample pretty output]\n" + pretty.indent(4).stripTrailing());

        // Pretty empty containers
        check("Pretty []", Json.stringifyPretty(JsonArray.empty()).equals("[]"));
        check("Pretty {}", Json.stringifyPretty(JsonObject.empty()).equals("{}"));

        // Pretty vs compact — same values
        JsonValue v = Json.parse("{\"x\":1}");
        check("Compact vs pretty same value",
            Json.parse(Json.stringify(v)).equals(Json.parse(Json.stringifyPretty(v))));

        // writePretty to Appendable
        StringBuilder sb = new StringBuilder();
        Json.writePretty(Json.parse("[1,2]"), sb);
        check("writePretty Appendable has newline", sb.toString().contains("\n"));

        // null rejection
        expectThrow("stringifyPretty null", () -> Json.stringifyPretty(null));
    }

    // ─────────────────────────────────────────────────────────────
    // FEATURE 5: FILE API
    // ─────────────────────────────────────────────────────────────
    static void feature5_FileAPI() throws Exception {
        banner("FEATURE 5: File API — Json.read / writeFile / writePrettyFile");

        Path tmp = Path.of("demo", "output");
        Files.createDirectories(tmp);

        // Read existing demo files
        JsonValue obj = Json.read(Path.of("demo", "data", "valid_object.json"));
        check("Read file: name",    obj.asObject().getString("name").equals("JValue"));
        check("Read file: version", obj.asObject().getInt("version") == 1);
        check("Read file: stable",  obj.asObject().getBoolean("stable"));
        check("Read file: null author", obj.asObject().get("author").isNull());

        JsonValue users = Json.read(Path.of("demo", "data", "users.json"));
        check("Read users: total", users.asObject().getInt("total") == 2);
        check("Read users: first name",
            users.asObject().getArray("users").getObject(0).getString("name").equals("Ada Lovelace"));

        // Write compact file and read back
        Path compact = tmp.resolve("compact_out.json");
        Json.writeFile(obj, compact);
        JsonValue readBack = Json.read(compact);
        check("Compact write/read roundtrip", readBack.equals(obj));
        System.out.println("  compact_out.json => " + Files.readString(compact));

        // Write pretty file and read back
        Path pretty = tmp.resolve("pretty_out.json");
        Json.writePrettyFile(obj, pretty);
        String prettyStr = Files.readString(pretty);
        check("Pretty file has newlines", prettyStr.contains("\n"));
        check("Pretty file round-trips",  Json.parse(prettyStr).equals(obj));
        System.out.println("  pretty_out.json =>\n" + prettyStr);

        // Read edge_numbers.json
        JsonValue nums = Json.read(Path.of("demo", "data", "edge_numbers.json"));
        check("edge_numbers: integer",       nums.asObject().getInt("integer") == 42);
        check("edge_numbers: -0 raw",        nums.asObject().get("negative_zero").asJsonNumber().raw().equals("-0"));
        check("edge_numbers: exponent",      nums.asObject().getDouble("exponent_lower") == 1e10);

        // Read edge_strings.json
        JsonValue strs = Json.read(Path.of("demo", "data", "edge_strings.json"));
        check("edge_strings: escaped quote", strs.asObject().getString("escaped_quote").equals("She said \"hello\""));
        check("edge_strings: escaped slash", strs.asObject().getString("escaped_slash").equals("http://example.com"));
        check("edge_strings: unicode emoji", strs.asObject().getString("unicode_emoji_surrogate").length() == 2);

        // Missing file throws IOException
        expectThrow("Missing file IOException", () -> {
            try { Json.read(Path.of("no_such_file.json")); }
            catch (java.io.IOException e) { throw new RuntimeException(e); }
        });

        // Null argument rejection
        expectThrow("read null path",  () -> { try { Json.read(null); } catch(Exception e) { throw new RuntimeException(e); }});
    }

    // ─────────────────────────────────────────────────────────────
    // FEATURE 6: JSON POINTER (RFC 6901)
    // ─────────────────────────────────────────────────────────────
    static void feature6_JsonPointer() throws Exception {
        banner("FEATURE 6: JsonPointer — RFC 6901 Read-Only Lookup");

        JsonValue users = Json.read(Path.of("demo", "data", "users.json"));
        JsonValue pe    = Json.read(Path.of("demo", "data", "pointer_edge.json"));

        // Empty pointer = root
        check("Empty pointer = root", Json.pointer(users, "").equals(users));

        // Basic navigation
        check("Pointer /total",             Json.pointer(users, "/total").asInt() == 2);
        check("Pointer /users/0/name",      Json.pointer(users, "/users/0/name").asString().equals("Ada Lovelace"));
        check("Pointer /users/1/role",      Json.pointer(users, "/users/1/role").asString().equals("member"));
        check("Pointer /users/0/active",    Json.pointer(users, "/users/0/active").asBoolean());
        check("Pointer /users/0/tags/2",    Json.pointer(users, "/users/0/tags/2").asString().equals("pioneer"));
        check("Pointer /users/0/address/city", Json.pointer(users, "/users/0/address/city").asString().equals("London"));

        // Escape sequences
        check("Pointer ~1 = /",     Json.pointer(pe, "/a~1b").asString().equals("slash key value"));
        check("Pointer ~0 = ~",     Json.pointer(pe, "/m~0n").asString().equals("tilde key value"));

        // Deep nesting
        check("Deep /c/d/e",        Json.pointer(pe, "/c/d/e").asString().equals("deep value"));

        // Array indexing
        check("Array /arr/0",       Json.pointer(pe, "/arr/0").asInt() == 10);
        check("Array /arr/3/0",     Json.pointer(pe, "/arr/3/0").asInt() == 40);

        // Empty string key
        check("Empty key /empty_key/", Json.pointer(pe, "/empty_key/").asString().equals("empty string key value"));

        // Optional API — missing key returns empty
        Optional<JsonValue> missing = Json.pointerOptional(users, "/users/99/name");
        check("pointerOptional missing = empty", missing.isEmpty());

        Optional<JsonValue> found = Json.pointerOptional(users, "/total");
        check("pointerOptional found = value", found.isPresent() && found.get().asInt() == 2);

        // Compiled pointer (reusable)
        JsonPointer compiled = JsonPointer.compile("/users/0/name");
        check("Compiled pointer query", compiled.query(users).asString().equals("Ada Lovelace"));

        // Invalid array indexes are unresolved
        expectThrow("Bad index -1",  () -> Json.pointer(pe, "/arr/-1"));
        expectThrow("Bad index 01",  () -> Json.pointer(pe, "/arr/01"));
        expectThrow("Missing key",   () -> Json.pointer(users, "/users/0/nonexistent"));

        // Malformed pointer syntax
        expectThrow("No leading /",  () -> Json.pointer(users, "total"));
        expectThrow("Bad ~2 escape", () -> Json.pointer(users, "/a~2b"));
        expectThrow("Null pointer",  () -> Json.pointer(users, null));

        // Root array pointer
        JsonValue arr = Json.read(Path.of("demo", "data", "root_array.json"));
        check("Root array /0",      Json.pointer(arr, "/0").asInt() == 1);
        check("Root array /1",      Json.pointer(arr, "/1").asString().equals("two"));
        check("Root array /4/nested", Json.pointer(arr, "/4/nested").asString().equals("object"));
    }

    // ─────────────────────────────────────────────────────────────
    // FEATURE 7: ERROR HANDLING
    // ─────────────────────────────────────────────────────────────
    static void feature7_ErrorHandling() {
        banner("FEATURE 7: Error Handling — JsonParseException positions");

        // Line/column/offset on parse errors
        try {
            Json.parse("{\n  \"bad\": [1, 2}\n}");
            fail("Should have thrown");
        } catch (JsonParseException e) {
            check("Exception has line",   e.line() >= 1);
            check("Exception has column", e.column() >= 1);
            check("Exception has offset", e.offset() >= 0);
            check("Exception message not empty", !e.getMessage().isEmpty());
            System.out.println("  Parse error: " + e.getMessage()
                + " at line=" + e.line() + " col=" + e.column() + " offset=" + e.offset());
        }

        // Error on first char
        try {
            Json.parse("bad");
            fail("Should have thrown");
        } catch (JsonParseException e) {
            check("Error at line 1", e.line() == 1);
            check("Error at col 1",  e.column() == 1);
            check("Error at offset 0", e.offset() == 0);
        }

        // ClassCastException when wrong type cast
        expectThrow("Wrong cast string->int", () -> Json.parse("\"hello\"").asInt());

        // NoSuchElementException for missing object key
        expectThrow("Missing key NoSuchElement", () ->
            Json.parse("{\"a\":1}").asObject().getString("z"));
    }

    // ─────────────────────────────────────────────────────────────
    // FEATURE 8: ROUND-TRIP
    // ─────────────────────────────────────────────────────────────
    static void feature8_RoundTrip() throws Exception {
        banner("FEATURE 8: Round-Trip — parse → stringify → parse");

        String[] inputs = {
            "null", "true", "false", "42", "-7", "3.14", "-0", "1e10",
            "\"hello world\"", "\"\"", "[]", "{}", "[1,2,3]",
            "{\"a\":1,\"b\":true,\"c\":null}",
            "{\"nested\":{\"arr\":[1,\"two\",null,true]}}",
        };

        for (String input : inputs) {
            JsonValue parsed    = Json.parse(input);
            String    compact   = Json.stringify(parsed);
            JsonValue reparsed  = Json.parse(compact);
            check("Round-trip: " + input, parsed.equals(reparsed));
        }

        // File round-trip
        JsonValue original = Json.read(Path.of("demo", "data", "users.json"));
        Path out = Path.of("demo", "output", "roundtrip.json");
        Files.createDirectories(out.getParent());
        Json.writePrettyFile(original, out);
        JsonValue restored = Json.read(out);
        check("File round-trip users.json", original.equals(restored));
    }

    // ─────────────────────────────────────────────────────────────
    // FEATURE 9: BUILD JSON FROM JAVA OBJECTS (No Jackson needed!)
    // ─────────────────────────────────────────────────────────────
    static void feature9_BuildFromJava() throws Exception {
        banner("FEATURE 9: Build JSON from Java Objects (Jackson-free!)");

        // Simulate: building an API response object in Java and serializing to JSON
        // This is the core hackathon use-case: "remove Jackson for this task"

        // Build a user object
        JsonObject address = JsonObject.of(
            "city",    JsonValue.of("Bangalore"),
            "country", JsonValue.of("India")
        );

        JsonArray tags = JsonArray.of(
            JsonValue.of("java"),
            JsonValue.of("zero-deps"),
            JsonValue.of("hackathon")
        );

        // Build via parse (JsonObject constructor is package-private by design)
        // This also demonstrates that stringify works for nested built objects
        String addressJson = Json.stringify(address);
        String tagsJson    = Json.stringify(tags);
        JsonObject user = Json.parse("""
            {
                "id": 101,
                "name": "Devansh Kant",
                "active": true,
                "score": 98.5,
                "address": %s,
                "tags": %s,
                "notes": null
            }
            """.formatted(addressJson, tagsJson)).asObject();

        String compact = Json.stringify(user);
        String pretty  = Json.stringifyPretty(user);

        check("Built object has id",     user.getInt("id") == 101);
        check("Built object has name",   user.getString("name").equals("Devansh Kant"));
        check("Built object active",     user.getBoolean("active"));
        check("Built object score",      user.getDouble("score") == 98.5);
        check("Built object tags size",  user.getArray("tags").size() == 3);
        check("Built object notes null", user.get("notes").isNull());
        check("Compact has id",          compact.contains("\"id\""));
        check("Pretty has newlines",     pretty.contains("\n"));

        System.out.println("\n  [Java Object → JSON (compact)]");
        System.out.println("  " + compact);
        System.out.println("\n  [Java Object → JSON (pretty)]");
        System.out.println(pretty.indent(2).stripTrailing());

        // Write to file
        Path out = Path.of("demo", "output", "built_user.json");
        Files.createDirectories(out.getParent());
        Json.writePrettyFile(user, out);
        JsonValue readBack = Json.read(out);
        check("Write+read Java-built object", readBack.equals(user));

        System.out.println("\n  Wrote to: " + out.toAbsolutePath());
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────
    static void banner(String title) {
        System.out.println("\n┌─ " + title);
    }

    static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  ✔ PASS  " + name);
        } else {
            failed++;
            System.out.println("  ✘ FAIL  " + name);
        }
    }

    static void fail(String msg) {
        failed++;
        System.out.println("  ✘ FAIL  " + msg);
    }

    static void expectThrow(String name, Runnable action) {
        try {
            action.run();
            failed++;
            System.out.println("  ✘ FAIL  " + name + " (no exception thrown)");
        } catch (Throwable t) {
            passed++;
            System.out.println("  ✔ PASS  " + name + " → " + t.getClass().getSimpleName());
        }
    }
}
