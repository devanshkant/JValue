# JValue - Zero-Dependency JSON Toolkit for Java 25

> The JSON parser the JDK does not ship.

JValue is a lightweight, dependency-free JSON toolkit for Java 25, built for
ZeroDepsHack 2026 Track B: Parsers & Data Formats.

## Current Features

- Parsing: RFC 8259 JSON parser with detailed errors: line, column, and offset.
- JSON value model: sealed hierarchy for object, array, string, number, boolean, and null.
- Number fidelity: JSON numbers preserve their original lexical representation.
- Validation coverage: hand-written unit tests plus JSONTestSuite conformance checks.

Planned future phases include serialization, JSON Pointer, and file I/O. They are not implemented yet.

All current production code uses only the Java standard library. No Jackson. No Gson. No dependencies.

## Why

Java's standard library does not provide a JSON implementation. Java developers commonly add
third-party dependencies such as Jackson or Gson for parsing and serialization. JValue implements
the core JSON tree parser by hand with JDK APIs only.

## Quick Start

```java
import com.jvalue.*;

JsonValue value = Json.parse("""
    {
        "name": "JValue",
        "version": 1,
        "features": ["parsing", "value-model", "error-reporting"],
        "zeroDeps": true
    }
    """);

JsonObject obj = value.asObject();
String name = obj.getString("name");           // "JValue"
int version = obj.getInt("version");           // 1
boolean zeroDeps = obj.getBoolean("zeroDeps"); // true
String firstFeature = obj.getArray("features").getString(0);

JsonObject built = JsonObject.of(
    "tool", JsonValue.of("JValue"),
    "deps", JsonValue.of(0)
);
```

## Build & Run

Requirements: Java 25 LTS, tested with JDK 25.0.4.

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

## Dependency Verification

This project has zero third-party production dependencies.

1. There is no `pom.xml`, `build.gradle`, or runtime dependency manifest.
2. Production source uses only JDK APIs.
3. Run `build.bat deps-proof` or `./build.sh deps-proof` to confirm.
4. See [deps-proof.txt](deps-proof.txt) for recorded proof output.

## Supported JSON Behavior

Current parser behavior includes objects, arrays, strings, numbers, booleans, null, JSON whitespace,
nested values, escape sequences, Unicode escapes, strict number grammar, duplicate object keys with
last-value-wins semantics, and a nesting depth limit of 512.

## Error Handling

Invalid JSON throws `JsonParseException`, which exposes `line()`, `column()`, and `offset()` in
addition to a human-readable message.

## Testing

Tests use a hand-written harness because the JDK does not ship JUnit. The suite covers the value
model, parser edge cases, error positions, depth limits, and the JSONTestSuite `test_parsing`
corpus when present locally.

## Limitations

- Serialization is not implemented yet.
- JSON Pointer is not implemented yet.
- File I/O convenience APIs are not implemented yet.
- The parser currently accepts `String` input only.

## STDLIB.md

See [STDLIB.md](STDLIB.md) for the standard-library substitution ledger.

## License

MIT
