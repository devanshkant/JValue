# JValue - Zero-Dependency JSON Toolkit for Java 25

> A small JSON parser and serializer for Java, built with only the JDK.

JValue is a lightweight JSON toolkit for Java 25, built for ZeroDepsHack 2026
Track B: Parsers & Data Formats. It provides an explicit JSON value model,
strict RFC 8259 parsing, compact serialization, fixed-format pretty
serialization, JSON Pointer lookup, and file convenience APIs, without Jackson,
Gson, Maven, Gradle, or any third-party runtime dependency.

It is not intended to replace the full feature set of Jackson or Gson. The goal
is a focused, dependency-free tree parser and serializer for lightweight JSON
workloads.

## Why

Java's standard library does not include a JSON implementation. Java developers
usually add third-party dependencies such as Jackson or Gson for parsing,
serialization, and JSON tree handling. JValue implements that core tree workflow
by hand using Java 25 and standard JDK APIs only.

## Requirements

- Java 25 LTS.
- Tested with JDK 25.0.4.
- No third-party production dependencies.

## Current Features

- RFC 8259 JSON parsing from `String`.
- JSON value model: `JsonObject`, `JsonArray`, `JsonString`, `JsonNumber`,
  `JsonBoolean`, and `JsonNull`.
- Detailed parse diagnostics through `JsonParseException`: line, column, and
  character offset.
- Strict string handling: JSON escapes, Unicode escapes, control character
  rejection, and surrogate-pair validation.
- Strict number grammar with raw lexical preservation.
- Compact serialization with `Json.stringify(...)`.
- Pretty serialization with `Json.stringifyPretty(...)`.
- Appendable-based output with `Json.write(...)` and `Json.writePretty(...)`.
- UTF-8 file convenience APIs with `Json.read(...)`, `Json.writeFile(...)`,
  and `Json.writePrettyFile(...)`.
- Read-only JSON Pointer lookup with `JsonPointer` and `Json.pointer(...)`.
- Explicit charset overloads for file reading and writing.
- Object iteration order preservation for predictable serialization.
- Hand-written tests plus JSONTestSuite conformance coverage.
- Command-line interface (CLI) for validation, formatting, and JSON Pointer inspection.

## Build & Run

```bash
# Build
./build.sh build        # Unix/macOS
build.bat build         # Windows

# Run tests
./build.sh test         # Unix/macOS
build.bat test          # Windows

# Verify zero production dependencies
./build.sh deps-proof   # Unix/macOS
build.bat deps-proof    # Windows
```

No Maven. No Gradle. Just `javac`.

## Quick Start (CLI)
First build the project using the commands in the Build & Run section above.
After a successful build, use the CLI wrapper to validate, format, and inspect JSON files.

```bash
$ ./jv.sh validate demo/data/users.json
  (reading from file: /.../demo/data/users.json)
  ✔  Valid JSON
  Root type : object
  Keys      : 3

$ ./jv.sh get demo/data/users.json /users/0/name
  (reading from file: /.../demo/data/users.json)
  Pointer : /users/0/name
  Type    : string
  Value   : 
"Ada Lovelace"

$ ./jv.sh compact demo/data/users.json
  (reading from file: /.../demo/data/users.json)
  {"users":[{"id":1,"name":"Ada Lovelace","role":"admin","active":true,"score":99.5,"address":{"city":"London","country":"UK"},"tags":["math","programming","pioneer"]},{"id":2,"name":"Alan Turing","role":"member","active":false,"score":100.0,"address":{"city":"London","country":"UK"},"tags":["computing","cryptography"]}],"total":2,"page":1}
```

Windows users should use `.\jv.bat` instead of `./jv.sh`. Run `./jv.sh help` to see all 15 commands.

## Quick Start (Java API)

```java
import com.jvalue.*;

JsonValue value = Json.parse("""
    {
        "name": "JValue",
        "version": 1,
        "features": ["parse", "stringify", "pretty"],
        "zeroDeps": true
    }
    """);

JsonObject obj = value.asObject();
String name = obj.getString("name");           // "JValue"
int version = obj.getInt("version");           // 1
boolean zeroDeps = obj.getBoolean("zeroDeps"); // true
String firstFeature = obj.getArray("features").getString(0);

String compact = Json.stringify(value);
String pretty = Json.stringifyPretty(value);
String firstFeatureByPointer = Json.pointer(value, "/features/0").asString();

Path path = Path.of("example.json");
Json.writePrettyFile(value, path);
JsonValue fromFile = Json.read(path);
```

## JSON Pointer

JValue supports read-only JSON Pointer lookup as defined by RFC 6901. A pointer
is a compact string path into a parsed JSON tree:

```java
JsonValue document = Json.parse("""
    {
        "users": [
            { "name": "Ada", "role": "admin" }
        ],
        "a/b": "slash key",
        "m~n": "tilde key"
    }
    """);

String name = Json.pointer(document, "/users/0/name").asString(); // "Ada"
String slash = Json.pointer(document, "/a~1b").asString();        // "slash key"
String tilde = Json.pointer(document, "/m~0n").asString();        // "tilde key"
```

The empty pointer `""` returns the root value itself. Non-empty pointers must
start with `/`. Tokens are decoded using RFC 6901 escapes: `~1` means `/`, and
`~0` means `~`. Object tokens are used exactly as decoded, so numeric-looking
object keys such as `"0"` remain object keys. Array tokens must be `0` or a
non-zero digit followed by digits; leading zeros, negative indexes, `+1`, `-`,
non-numeric tokens, overflow, and out-of-bounds indexes do not resolve.

Use `JsonPointer.compile(pointer)` when applying the same pointer repeatedly:

```java
JsonPointer pointer = JsonPointer.compile("/users/0/role");
JsonValue role = pointer.query(document);
```

Missing targets throw `NoSuchElementException` from required lookup APIs and
return `Optional.empty()` from optional lookup APIs:

```java
JsonValue required = Json.pointer(document, "/users/0/name");
Optional<JsonValue> maybe = Json.pointerOptional(document, "/users/1/name");
```

Malformed pointer syntax throws `IllegalArgumentException`, and Java `null`
arguments throw `NullPointerException`. JSON Pointer support is lookup-only:
there is no JSON Patch, mutation, wildcard query, recursive descent, or schema
validation.

## Serialization

Compact output:

```java
JsonValue value = JsonObject.of(
    "tool", JsonValue.of("JValue"),
    "deps", JsonValue.of(0)
);

String json = Json.stringify(value);
// {"tool":"JValue","deps":0}
```

Pretty output uses two-space indentation and no trailing newline:

```java
String pretty = Json.stringifyPretty(value);
// {
//   "tool": "JValue",
//   "deps": 0
// }
```

For caller-owned output sinks, use `Appendable`:

```java
StringBuilder out = new StringBuilder();
Json.write(value, out);

StringBuilder prettyOut = new StringBuilder();
Json.writePretty(value, prettyOut);
```

For files, use the `Path` convenience APIs. UTF-8 is the default, and explicit
charset overloads are available when needed:

```java
Path path = Path.of("data.json");

JsonValue value = Json.read(path);
Json.writeFile(value, path);
Json.writePrettyFile(value, path);

JsonValue latin1 = Json.read(path, StandardCharsets.ISO_8859_1);
Json.writeFile(latin1, path, StandardCharsets.ISO_8859_1);
```

Numbers are serialized from `JsonNumber.raw()` so parsed numeric spellings such
as `-0`, `1.0`, `1e0`, and `1E0` are preserved.

## Error Handling

Invalid JSON throws `JsonParseException`, which exposes `line()`, `column()`,
and `offset()`:

```java
try {
    Json.parse("{\"missing\": [1, 2}");
} catch (JsonParseException e) {
    System.out.println(e.getMessage());
    System.out.println(e.line());
    System.out.println(e.column());
    System.out.println(e.offset());
}
```

Serialization throws `NullPointerException` for Java `null` inputs and
`IllegalArgumentException` if a manually constructed value contains invalid
internal state, such as an invalid raw number or a lone surrogate code unit in a
string.

File APIs propagate `IOException` from JDK file operations. File reading does
not strip a UTF-8 BOM; it preserves the existing parser policy, so a leading
U+FEFF is rejected as an invalid JSON starting character.

JSON Pointer lookup throws `IllegalArgumentException` for malformed pointer
syntax. A syntactically valid pointer that cannot be resolved throws
`NoSuchElementException` from `query(...)` / `Json.pointer(...)`, or returns
`Optional.empty()` from `queryOptional(...)` / `Json.pointerOptional(...)`.

## Supported JSON Behavior

JValue supports JSON objects, arrays, strings, numbers, booleans, null, JSON
whitespace, nested values, escape sequences, Unicode escapes, strict number
grammar, duplicate object keys with last-value-wins semantics, a nesting depth
limit of 512, and read-only RFC 6901 JSON Pointer lookup.

## Testing

Tests use a hand-written harness because the JDK does not ship JUnit. The suite
covers the value model, parser edge cases, error positions, depth limits,
serialization, pretty printing, Appendable output, JSON Pointer lookup, round
trips, and the JSONTestSuite `test_parsing` corpus when present locally.

Current verified results:

- Hand-written tests: 186 passed, 0 failed, 0 errors.
- JSONTestSuite: 305 passed, 0 failed, 13 skipped.

The skipped JSONTestSuite cases are byte-level encoding cases that are not
applicable to the current `String`-based parser.

## Dependency Verification

This project has zero third-party production dependencies.

1. There is no `pom.xml`, `build.gradle`, or runtime dependency manifest.
2. Production source uses only JDK APIs.
3. Run `build.bat deps-proof` or `./build.sh deps-proof`.
4. `jdeps` confirms the production classes depend only on `java.base`.
5. See [deps-proof.txt](deps-proof.txt) for recorded proof output.

## Limitations

- JSON Pointer support is read-only lookup; JSON Patch and mutation are not implemented.
- Reader/InputStream parsing is not implemented.
- File APIs read whole files into memory before parsing.
- Serialization is tree-based, not streaming from arbitrary Java objects.
- File APIs do not create parent directories automatically.
- There is no POJO binding, schema validation, comments, JSON5, YAML, or TOML.

## STDLIB.md

See [STDLIB.md](STDLIB.md) for the standard-library substitution ledger.

## License

MIT
