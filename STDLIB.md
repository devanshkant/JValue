# STDLIB.md — Standard-Library Substitutions

This document records every case where JValue uses the Java standard library (`java.base` module)
instead of a third-party dependency that a typical Java developer would reach for.

Each entry names the third-party package that would normally be used, the JDK API used instead,
and a brief explanation of the substitution.

Entries are added continuously as each substitution is implemented. Only implemented and verifiable
substitutions appear in the main list. Future substitutions are listed separately below.

---

## Implemented Substitutions

### 1. Test Framework — JUnit 5 → Hand-Written Test Harness

**Normally:** `org.junit.jupiter` (JUnit 5) for test discovery, execution, and reporting.

**Instead:** A hand-written test harness in `TestRunner.java` using structured methods called from `main()`.

**How:** `TestRunner.runTest(String, Runnable)` wraps each test method, catches `AssertionError` for test failures and `Exception` for errors, and accumulates pass/fail/error counts. `main()` reports results and exits with code 1 if any test failed.

**JDK APIs used:** `java.lang.Runnable`, `java.lang.AssertionError`, `System.out`, `System.exit(int)`.

**Verifiable in:** [`src/test/java/com/jvalue/test/TestRunner.java`](src/test/java/com/jvalue/test/TestRunner.java)

**Tradeoff:** No annotation-based discovery, no parameterized test sugar, no IDE integration. This is the required approach for Java under this hackathon's rules — the JDK ships no test framework (confirmed by the event cheat sheet). The use of a hand-written harness is disclosed here as required.

---

### 2. Assertion Library — AssertJ / Hamcrest → Hand-Written Assertions

**Normally:** `org.assertj.core.api.Assertions` (AssertJ) or `org.hamcrest.Matchers` (Hamcrest) for fluent, descriptive test assertions.

**Instead:** Hand-written static assertion methods in `TestRunner.java`: `assertEquals`, `assertTrue`, `assertFalse`, `assertNull`, `assertNotNull`, `assertThrows`.

**How:** Each method checks its condition and throws `AssertionError` with a descriptive message on failure. `assertThrows` catches the expected throwable type and fails if the wrong type (or nothing) is thrown.

**JDK APIs used:** `java.lang.AssertionError`, `java.lang.Class.isInstance(Object)`, `java.lang.Class.getSimpleName()`.

**Verifiable in:** [`src/test/java/com/jvalue/test/TestRunner.java`](src/test/java/com/jvalue/test/TestRunner.java) lines 70–116.

**Tradeoff:** Less expressive than AssertJ's fluent API. Sufficient for comprehensive table-driven testing of a JSON toolkit.

---

### 3. JSON Value Model — Jackson `JsonNode` / Gson `JsonElement` → Sealed Interface Hierarchy

**Normally:** Jackson's `com.fasterxml.jackson.databind.JsonNode` tree model or Gson's `com.google.gson.JsonElement`.

**Instead:** A sealed `JsonValue` interface hierarchy using sealed interfaces (Java 17+) with concrete types `JsonObject`, `JsonArray`, `JsonString`, `JsonNumber`, `JsonBoolean`, `JsonNull`.

**How:** Java's sealed types enforce at compile time that all JSON value kinds are handled exhaustively. Pattern matching in `switch` expressions (Java 21+) enables clean dispatch without `instanceof` chains.

**JDK APIs used:** `sealed`, `permits` (Java language features).

**Verifiable in:** [`src/main/java/com/jvalue/JsonValue.java`](src/main/java/com/jvalue/JsonValue.java)

**Tradeoff:** The sealed hierarchy is more type-safe than Jackson's `JsonNode` but does not support Jackson's `ObjectMapper` deserialization into POJOs.

---

### 4. Ordered Key-Value Store — Guava `ImmutableMap` → `java.util.LinkedHashMap`

**Normally:** `com.google.common.collect.ImmutableMap` from Guava.

**Instead:** `java.util.Collections.unmodifiableMap()` wrapping a `java.util.LinkedHashMap`.

**How:** `JsonObject` uses `LinkedHashMap` internally to preserve key insertion order (crucial for predictable JSON serialization), and exposes it via `Collections.unmodifiableMap()` to prevent external mutation after parsing.

**JDK APIs used:** `java.util.LinkedHashMap`, `java.util.Collections.unmodifiableMap()`.

**Verifiable in:** [`src/main/java/com/jvalue/JsonObject.java`](src/main/java/com/jvalue/JsonObject.java)

**Tradeoff:** `Collections.unmodifiableMap()` wraps the original map (not a true copy) but since the toolkit controls the internal map's reference, safety is guaranteed.

---

### 5. JSON Parsing — Jackson `ObjectMapper` / Gson `JsonParser` → Hand-Written Recursive-Descent Parser

**Normally:** `com.fasterxml.jackson.databind.ObjectMapper.readTree()` or `com.google.gson.JsonParser.parseString()` for parsing JSON text into a tree model.

**Instead:** A hand-written recursive-descent parser implementing the full RFC 8259 JSON grammar.

**How:** `CharSource` wraps a `String` input and provides character-by-character access with line/column/offset tracking. `JsonParser` implements one method per JSON production rule (`parseValue`, `parseString`, `parseNumber`, `parseArray`, `parseObject`, `parseNull`, `parseBoolean`). The parser handles all RFC 8259 escape sequences, Unicode escapes with surrogate pair validation, strict number grammar enforcement, nesting depth limits (512 levels), and detailed error reporting with position information.

**JDK APIs used:** `java.lang.String`, `java.lang.Character` (surrogate detection), `java.lang.StringBuilder`, `java.util.ArrayList`, `java.util.LinkedHashMap`.

**Verifiable in:** [`src/main/java/com/jvalue/JsonParser.java`](src/main/java/com/jvalue/JsonParser.java), [`src/main/java/com/jvalue/CharSource.java`](src/main/java/com/jvalue/CharSource.java), [`src/main/java/com/jvalue/JsonParseException.java`](src/main/java/com/jvalue/JsonParseException.java)

**Tradeoff:** No streaming/incremental parsing (Phase 7 candidate). No `Reader`-based input yet (Phase 6). Handles only `String` input. Does not support POJO deserialization like Jackson's `ObjectMapper`. The parser is strict RFC 8259: it rejects lone surrogates, unescaped control characters, leading zeros, and trailing data.

---

### 6. Unicode Escape Handling — ICU4J → `java.lang.Character`

**Normally:** ICU4J (`com.ibm.icu.lang.UCharacter`) or similar libraries for Unicode-aware character processing.

**Instead:** `java.lang.Character` static methods for surrogate pair handling.

**How:** The JSON string parser validates `\uXXXX` escape sequences by checking for high/low surrogates using `Character.isHighSurrogate()`, `Character.isLowSurrogate()`, and assembling code points via `Character.toCodePoint()` and `Character.toChars()`. Unescaped control characters (U+0000–U+001F) are detected via direct char comparison.

**JDK APIs used:** `java.lang.Character.isHighSurrogate()`, `java.lang.Character.isLowSurrogate()`, `java.lang.Character.toCodePoint()`, `java.lang.Character.toChars()`.

**Verifiable in:** [`src/main/java/com/jvalue/JsonParser.java`](src/main/java/com/jvalue/JsonParser.java) (method `parseUnicodeEscape`)

**Tradeoff:** No normalization (NFC/NFD), no grapheme cluster support. Sufficient for JSON string parsing per RFC 8259.

---


## Planned Substitutions - NOT IMPLEMENTED

The following substitutions are planned but not yet implemented. They will be moved to the main
list as each feature is completed and verifiable in source.
These entries are planning notes only. They are not implemented, do not constitute evidence of completed work, and must not be counted toward any hackathon bonus unless implemented.

| # | What | Replaces | JDK API |
|---|---|---|---|
| 8 | JSON serialization | Jackson `JsonGenerator` / Gson `JsonWriter` | `java.lang.StringBuilder`, `java.io.Writer` 
| 9 | File I/O | Apache Commons IO | `java.nio.file.Files`, `java.nio.file.Path`
| 10 | Data carrier objects | Lombok `@Value` | Java records (Java 16+)
| 11 | JSON Pointer (RFC 6901) | Jackson `JsonPointer` / `json-pointer` lib | `String.split()`, `Integer.parseInt()`, `String.replace()`
| 12 | Pretty printing | Jackson `DefaultPrettyPrinter` / Gson `setPrettyPrinting()` | `java.lang.StringBuilder`, depth counter 

> **Note on `HexFormat`:** `java.util.HexFormat` (Java 17+) was considered for Unicode escape parsing
> but the implementation uses direct hex digit conversion via a custom `hexDigit(char)` method, which
> is simpler and avoids unnecessary object allocation. This does not warrant a STDLIB entry.

---

*This document is maintained continuously during development. Every substitution in the main list
is implemented and verifiable in the current source code.*

