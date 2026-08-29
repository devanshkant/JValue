# JValue — Zero-Dependency JSON Toolkit for Java 25

> The JSON parser the JDK doesn't ship.

[![Track B — Parsers & Data Formats](https://img.shields.io/badge/Track-B%20%E2%80%94%20Parsers%20%26%20Data%20Formats-blue)]()
[![Zero Dependencies](https://img.shields.io/badge/Dependencies-Zero-brightgreen)]()
[![Java 25](https://img.shields.io/badge/Java-25%20LTS-orange)]()

## What It Does

JValue is a lightweight, dependency-free JSON toolkit for Java 25. It provides:

- **Parsing** — Full RFC 8259 JSON parser with detailed error messages (line, column, offset)
- **JSON Value Model** — Type-safe sealed hierarchy: `JsonObject`, `JsonArray`, `JsonString`, `JsonNumber`, `JsonBoolean`, `JsonNull`
- **Serialization** — Compact and pretty-printed JSON output with correct escaping
- **JSON Pointer** — RFC 6901 deep access into JSON structures
- **File I/O** — Read from and write to files with a single call

All built using only the Java standard library (`java.base` module). No Jackson. No Gson. No dependencies.

## Why

Java is the only major language in this hackathon without a built-in JSON implementation:

| Language | JSON Support |
|---|---|
| JavaScript | `JSON.parse()` / `JSON.stringify()` |
| Python | `json` module |
| Go | `encoding/json` (v2 in 1.27) |
| C# / .NET | `System.Text.Json` |
| **Java** | **None** |

Every Java project that touches JSON adds a third-party dependency. JValue is what the JDK should ship.

## Quick Start

```java
import com.jvalue.*;

// Parse JSON
JsonValue value = Json.parse("""
    {
        "name": "JValue",
        "version": 1,
        "features": ["parsing", "serialization", "json-pointer"],
        "zeroDeps": true
    }
    """);

// Access values
JsonObject obj = value.asObject();
String name = obj.getString("name");        // "JValue"
int version = obj.getInt("version");        // 1
boolean zeroDeps = obj.getBoolean("zeroDeps"); // true

// JSON Pointer (RFC 6901)
JsonValue feature = value.at("/features/0"); // "parsing"

// Pretty-print
System.out.println(value.toJson(JsonWriteOptions.pretty()));

// Build JSON programmatically
JsonObject built = JsonObject.of(
    "tool", "JValue",
    "deps", 0
);
```

## Build & Run

**Requirements:** Java 25 LTS (JDK 25.0.4+)

```bash
# Build
./build.sh build        # Unix/macOS
build.bat build          # Windows

# Run tests
./build.sh test          # Unix/macOS
build.bat test           # Windows

# Verify zero dependencies
./build.sh deps-proof    # Unix/macOS
build.bat deps-proof     # Windows
```

No Maven. No Gradle. Just `javac`.

## Dependency Verification

This project has **zero third-party dependencies**. Verification:

1. There is no `pom.xml`, `build.gradle`, or dependency manifest
2. All source files use only `java.base` module imports
3. Run `build.bat deps-proof` (or `./build.sh deps-proof`) to confirm
4. See [deps-proof.txt](deps-proof.txt) for the output

## Supported JSON Behavior

<!-- TODO: Fill in after implementation -->

## Error Handling

<!-- TODO: Fill in after implementation -->

## Testing

<!-- TODO: Fill in after implementation -->

## Architecture

<!-- TODO: Fill in after implementation -->

## Limitations

<!-- TODO: Fill in after implementation -->

## STDLIB.md

See [STDLIB.md](STDLIB.md) for a detailed account of every standard-library substitution.

## License

MIT

---

*Built for [ZeroDepsHack 2026](https://zerodepshack.com) — Track B: Parsers & Data Formats*
