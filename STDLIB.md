# STDLIB.md — Standard-Library Substitutions

This document records every case where JValue uses the Java standard library instead of a
third-party dependency that a typical Java developer would reach for. Current production
classes depend only on `java.base`; test-support utilities may use other JDK modules.

Each entry names the third-party package that would normally be used, the JDK API used instead,
and a brief explanation of the substitution.

Entries are added continuously as each substitution is implemented. Only implemented and verifiable
substitutions appear in the main list. Future substitutions are listed separately below.

---

## Implemented Substitutions

### 1. Test Framework — JUnit 5 → Hand-Written Test Harness

**Problem:** Run repeatable automated tests for the parser, value model, and serializer without a Java test framework dependency.

**Normally:** `org.junit.jupiter` (JUnit 5) for test discovery, execution, and reporting.

**Instead:** A hand-written test harness in `TestRunner.java` using structured methods called from `main()`.

**How:** `TestRunner.runTest(String, Runnable)` wraps each test method, catches `AssertionError` for test failures and `Exception` for errors, and accumulates pass/fail/error counts. `main()` reports results and exits with code 1 if any test failed.

**JDK APIs used:** `java.lang.Runnable`, `java.lang.AssertionError`, `System.out`, `System.exit(int)`.

**Verifiable in:** [`src/test/java/com/jvalue/test/TestRunner.java`](src/test/java/com/jvalue/test/TestRunner.java)

**Tradeoff:** No annotation-based discovery, no parameterized test sugar, no IDE integration. This is the required approach for Java under this hackathon's rules — the JDK ships no test framework (confirmed by the event cheat sheet). The use of a hand-written harness is disclosed here as required.

---

### 2. Assertion Library — AssertJ / Hamcrest → Hand-Written Assertions

**Problem:** Express test expectations and failure messages without pulling in a fluent assertion library.

**Normally:** `org.assertj.core.api.Assertions` (AssertJ) or `org.hamcrest.Matchers` (Hamcrest) for fluent, descriptive test assertions.

**Instead:** Hand-written static assertion methods in `TestRunner.java`: `assertEquals`, `assertTrue`, `assertFalse`, `assertNull`, `assertNotNull`, `assertThrows`.

**How:** Each method checks its condition and throws `AssertionError` with a descriptive message on failure. `assertThrows` catches the expected throwable type and fails if the wrong type (or nothing) is thrown.

**JDK APIs used:** `java.lang.AssertionError`, `java.lang.Class.isInstance(Object)`, `java.lang.Class.getSimpleName()`.

**Verifiable in:** [`src/test/java/com/jvalue/test/TestRunner.java`](src/test/java/com/jvalue/test/TestRunner.java) lines 70–116.

**Tradeoff:** Less expressive than AssertJ's fluent API. Sufficient for comprehensive table-driven testing of a JSON toolkit.

---

### 3. JSON Value Model — Jackson `JsonNode` / Gson `JsonElement` → Sealed Interface Hierarchy

**Problem:** Represent parsed JSON values explicitly without depending on a third-party JSON tree model.

**Normally:** Jackson's `com.fasterxml.jackson.databind.JsonNode` tree model or Gson's `com.google.gson.JsonElement`.

**Instead:** A sealed `JsonValue` interface hierarchy using sealed interfaces (Java 17+) with concrete types `JsonObject`, `JsonArray`, `JsonString`, `JsonNumber`, `JsonBoolean`, `JsonNull`.

**How:** Java's sealed types enforce at compile time that all JSON value kinds are handled exhaustively. Pattern matching in `switch` expressions (Java 21+) enables clean dispatch without `instanceof` chains.

**JDK APIs used:** `sealed`, `permits` (Java language features).

**Verifiable in:** [`src/main/java/com/jvalue/JsonValue.java`](src/main/java/com/jvalue/JsonValue.java)

**Tradeoff:** The sealed hierarchy is more type-safe than Jackson's `JsonNode` but does not support Jackson's `ObjectMapper` deserialization into POJOs.

---

### 4. Ordered Key-Value Store — Guava `ImmutableMap` → `java.util.LinkedHashMap`

**Problem:** Store JSON object members immutably enough for JValue while preserving predictable iteration order.

**Normally:** `com.google.common.collect.ImmutableMap` from Guava.

**Instead:** `java.util.Collections.unmodifiableMap()` wrapping a `java.util.LinkedHashMap`.

**How:** `JsonObject` uses `LinkedHashMap` internally to preserve key insertion order (crucial for predictable JSON serialization), and exposes it via `Collections.unmodifiableMap()` to prevent external mutation after parsing.

**JDK APIs used:** `java.util.LinkedHashMap`, `java.util.Collections.unmodifiableMap()`.

**Verifiable in:** [`src/main/java/com/jvalue/JsonObject.java`](src/main/java/com/jvalue/JsonObject.java)

**Tradeoff:** `Collections.unmodifiableMap()` wraps the original map (not a true copy) but since the toolkit controls the internal map's reference, safety is guaranteed.

---

### 5. JSON Parsing — Jackson `ObjectMapper` / Gson `JsonParser` → Hand-Written Recursive-Descent Parser

**Problem:** Parse JSON text into a value tree without using a third-party JSON parser.

**Normally:** `com.fasterxml.jackson.databind.ObjectMapper.readTree()` or `com.google.gson.JsonParser.parseString()` for parsing JSON text into a tree model.

**Instead:** A hand-written recursive-descent parser implementing the full RFC 8259 JSON grammar.

**How:** `CharSource` wraps a `String` input and provides character-by-character access with line/column/offset tracking. `JsonParser` implements one method per JSON production rule (`parseValue`, `parseString`, `parseNumber`, `parseArray`, `parseObject`, `parseNull`, `parseBoolean`). The parser handles all RFC 8259 escape sequences, Unicode escapes with surrogate pair validation, strict number grammar enforcement, nesting depth limits (512 levels), and detailed error reporting with position information.

**JDK APIs used:** `java.lang.String`, `java.lang.Character` (surrogate detection), `java.lang.StringBuilder`, `java.util.ArrayList`, `java.util.LinkedHashMap`.

**Verifiable in:** [`src/main/java/com/jvalue/JsonParser.java`](src/main/java/com/jvalue/JsonParser.java), [`src/main/java/com/jvalue/CharSource.java`](src/main/java/com/jvalue/CharSource.java), [`src/main/java/com/jvalue/JsonParseException.java`](src/main/java/com/jvalue/JsonParseException.java)

**Tradeoff:** No streaming/incremental parsing (Phase 7 candidate). The parser itself handles `String` input; file convenience APIs read the complete file into memory before delegating to it. Does not support POJO deserialization like Jackson's `ObjectMapper`. The parser is strict RFC 8259: it rejects lone surrogates, unescaped control characters, leading zeros, and trailing data.

---

### 6. Unicode Escape Handling — ICU4J → `java.lang.Character`

**Problem:** Validate JSON Unicode escape sequences and surrogate pairs without a Unicode helper library.

**Normally:** ICU4J (`com.ibm.icu.lang.UCharacter`) or similar libraries for Unicode-aware character processing.

**Instead:** `java.lang.Character` static methods for surrogate pair handling.

**How:** The JSON string parser validates `\uXXXX` escape sequences by checking for high/low surrogates using `Character.isHighSurrogate()`, `Character.isLowSurrogate()`, and assembling code points via `Character.toCodePoint()` and `Character.toChars()`. Unescaped control characters (U+0000–U+001F) are detected via direct char comparison.

**JDK APIs used:** `java.lang.Character.isHighSurrogate()`, `java.lang.Character.isLowSurrogate()`, `java.lang.Character.toCodePoint()`, `java.lang.Character.toChars()`.

**Verifiable in:** [`src/main/java/com/jvalue/JsonParser.java`](src/main/java/com/jvalue/JsonParser.java) (method `parseUnicodeEscape`)

**Tradeoff:** No normalization (NFC/NFD), no grapheme cluster support. Sufficient for JSON string parsing per RFC 8259.

---


### 7. Test Corpus Fetching - git submodule / curl / wget -> JDK HTTP and ZIP APIs

**Problem:** Acquire the optional JSONTestSuite conformance corpus without adding a submodule, curl/wget dependency, or build plugin.

**Normally:** A git submodule, `git clone`, `curl`, `wget`, or a build-tool plugin to fetch an external conformance corpus.

**Instead:** A small JDK-only test-support utility in `FetchCorpus.java`.

**How:** `FetchCorpus` downloads the JSONTestSuite archive with `java.net.http.HttpClient`, reads the response into bytes, extracts it with `java.util.zip.ZipInputStream`, and writes files with `java.nio.file.Files`. The downloaded corpus is ignored by Git and is not part of the production artifact.

**JDK APIs used:** `java.net.http.HttpClient`, `java.net.http.HttpRequest`, `java.net.http.HttpResponse`, `java.util.zip.ZipInputStream`, `java.util.zip.ZipEntry`, `java.nio.file.Files`, `java.nio.file.Path`.

**Verifiable in:** [`src/test/java/com/jvalue/test/FetchCorpus.java`](src/test/java/com/jvalue/test/FetchCorpus.java)

**Tradeoff:** This is test/support infrastructure only. It is not a runtime feature of the JSON library and does not change the production dependency proof.

---

### 8. JSON Serialization - Jackson `JsonGenerator` / Gson `JsonWriter` -> Hand-Written Serializer

**Problem:** Emit valid compact JSON text from the `JsonValue` tree without relying on a JSON writer package.

**Normally:** Jackson's `com.fasterxml.jackson.core.JsonGenerator` or Gson's `com.google.gson.stream.JsonWriter` for emitting JSON text from a value tree.

**Instead:** A hand-written compact serializer in `JsonSerializer.java` using `java.lang.StringBuilder` for string-returning serialization and `java.lang.Appendable` for caller-owned output sinks.

**How:** `JsonSerializer` recursively walks the sealed `JsonValue` hierarchy and writes RFC 8259 JSON tokens directly. `StringBuilder` is sufficient for `Json.stringify(...)` because it is an in-memory string-producing API. `Appendable` is sufficient for `Json.write(...)` because the serializer only needs ordered character output and can propagate `IOException` from sinks such as writers. Object keys and string values are escaped with standard character handling for quotes, backslashes, named control escapes, other U+0000-U+001F control characters, Unicode text, and surrogate-pair validation. Numbers are emitted from `JsonNumber.raw()` to preserve the existing lexical round-trip contract.

**JDK APIs used:** `java.lang.StringBuilder`, `java.lang.Appendable`, `java.lang.Character`, `java.io.IOException`.

**Verifiable in:** [`src/main/java/com/jvalue/JsonSerializer.java`](src/main/java/com/jvalue/JsonSerializer.java), [`src/test/java/com/jvalue/JsonSerializerTest.java`](src/test/java/com/jvalue/JsonSerializerTest.java)

**Tradeoff:** This entry covers tree serialization only. It does not provide Jackson/Gson-style object binding, streaming token APIs, configurable escaping, or schema support.

---

### 9. Pretty Printing - Jackson `DefaultPrettyPrinter` / Gson `setPrettyPrinting()` -> Depth Counter

**Problem:** Produce human-readable JSON from the same `JsonValue` tree without adding a formatting dependency.

**Normally:** Jackson's `DefaultPrettyPrinter` or Gson's `setPrettyPrinting()` option for human-readable JSON output.

**Instead:** A fixed pretty-printing mode in `JsonSerializer.java` using a recursive depth counter and `java.lang.Appendable`.

**How:** `JsonSerializer` emits non-empty arrays and objects across multiple lines, writes two spaces per nesting level, keeps empty containers compact, preserves object iteration order, and reuses the same string escaping and number validation logic as compact serialization. No external pretty-printer is required because JValue has a small fixed data model and a deliberately fixed output style.

**JDK APIs used:** `java.lang.StringBuilder`, `java.lang.Appendable`, primitive control flow.

**Verifiable in:** [`src/main/java/com/jvalue/JsonSerializer.java`](src/main/java/com/jvalue/JsonSerializer.java), [`src/test/java/com/jvalue/JsonSerializerTest.java`](src/test/java/com/jvalue/JsonSerializerTest.java)

**Tradeoff:** Formatting is intentionally fixed: two-space indentation, no root trailing newline, and no configurable printer options. This replaces only the pretty output needed by JValue, not the full configurability of Jackson or Gson.

---

### 10. JSON File Convenience APIs - Apache Commons IO / Jackson File Helpers -> `java.nio.file.Files`

**Problem:** Read JSON from files and write JSON back to files without requiring callers to install file utility packages or JSON library convenience wrappers.

**Normally:** Apache Commons IO (`FileUtils.readFileToString`, `FileUtils.writeStringToFile`) for simple file text operations, or Jackson/Gson file-oriented helpers layered on their parsers and writers.

**Instead:** Public `Json.read(...)`, `Json.writeFile(...)`, and `Json.writePrettyFile(...)` methods implemented with JDK file APIs and the existing JValue parser/serializer.

**How:** `Json.read(Path, Charset)` decodes a whole file with `Files.readString(path, charset)` and delegates to `Json.parse(String)`. `Json.writeFile(JsonValue, Path, Charset)` and `Json.writePrettyFile(JsonValue, Path, Charset)` open a buffered writer with `Files.newBufferedWriter(path, charset)` and delegate to `Json.write(...)` or `Json.writePretty(...)`. UTF-8 overloads use `StandardCharsets.UTF_8`.

**JDK APIs used:** `java.nio.file.Files`, `java.nio.file.Path`, `java.nio.charset.Charset`, `java.nio.charset.StandardCharsets`.

**Verifiable in:** [`src/main/java/com/jvalue/Json.java`](src/main/java/com/jvalue/Json.java), [`src/test/java/com/jvalue/test/JsonFileApiTest.java`](src/test/java/com/jvalue/test/JsonFileApiTest.java)

**Tradeoff:** These are convenience APIs, not streaming APIs. They read or write complete JSON documents and do not create parent directories or perform atomic replacement. File input preserves the existing parser policy; a leading BOM is not stripped before parsing.

---

### 11. JSON Pointer Navigation - Jackson `JsonPointer` / JSON Pointer Library -> JDK Collections and Strings

**Problem:** Locate nested values inside a parsed JSON tree using the standard RFC 6901 JSON Pointer syntax without requiring callers to install a larger JSON library.

**Normally:** Jackson's `com.fasterxml.jackson.core.JsonPointer`, Jackson tree traversal helpers, or a small standalone `json-pointer` library.

**Instead:** A compact immutable `JsonPointer` value object implemented with JDK collections and string handling, layered over JValue's existing `JsonValue` tree.

**How:** `JsonPointer.compile(String)` validates pointer syntax, preserves empty reference tokens, and decodes RFC 6901 escapes (`~0` and `~1`) in one pass. The compiled pointer stores decoded tokens in an immutable `List` and resolves them against `JsonObject` and `JsonArray` using the existing public tree APIs. Array index handling is implemented directly with character checks and overflow-safe integer accumulation.

**JDK APIs used:** `java.lang.String`, `java.lang.StringBuilder`, `java.util.ArrayList`, `java.util.List`, `java.util.Optional`, `java.util.NoSuchElementException`, `java.util.Objects`.

**Verifiable in:** [`src/main/java/com/jvalue/JsonPointer.java`](src/main/java/com/jvalue/JsonPointer.java), [`src/main/java/com/jvalue/Json.java`](src/main/java/com/jvalue/Json.java), [`src/test/java/com/jvalue/test/JsonPointerTest.java`](src/test/java/com/jvalue/test/JsonPointerTest.java)

**Tradeoff:** This is read-only RFC 6901 lookup. It intentionally does not implement JSON Patch, mutation, wildcard queries, recursive descent, schema validation, or a broader query language.

---

### 12. CLI Argument Parsing — picocli / Commons CLI → `String[] args`

**Problem:** Provide a feature-rich CLI wrapper without adding an argument-parsing library.

**Normally:** `info.picocli` (picocli) or `org.apache.commons.cli` for parsing flags, subcommands, and generating help menus.

**Instead:** Hand-written parsing using the native `String[] args` passed to `main()`.

**How:** `JValueCli.main(String[] args)` switches on `args[0]` using a Java 21+ `switch` expression to route to specific subcommand methods (e.g., `cmdValidate`, `cmdPretty`). Each method checks `args.length` and extracts required positional arguments by index. Help text and ANSI color formatting are generated via `System.out.println` and `String.format`.

**JDK APIs used:** `String[]`, `switch`, `System.out`, `System.exit`.

**Verifiable in:** [`cli/JValueCli.java`](cli/JValueCli.java)

**Tradeoff:** No automatic help generation, no long/short flag aliases (`--help` vs `-h`), and positional arguments are strictly ordered. This is a deliberate choice to keep the CLI wrapper dependency-free.

---


## Planned Substitutions - NOT IMPLEMENTED

The following substitutions are planned but not yet implemented. They will be moved to the main
list as each feature is completed and verifiable in source.
These entries are planning notes only. They are not implemented, do not constitute evidence of completed work, and must not be counted toward any hackathon bonus unless implemented.

| What | Replaces | Candidate JDK API |
|---|---|---|
| Data carrier objects | Lombok `@Value` | Java records (Java 16+)

> **Note on `HexFormat`:** `java.util.HexFormat` (Java 17+) was considered for Unicode escape parsing
> but the implementation uses direct hex digit conversion via a custom `hexDigit(char)` method, which
> is simpler and avoids unnecessary object allocation. This does not warrant a STDLIB entry.

---

*This document is maintained continuously during development. Every substitution in the main list
is implemented and verifiable in the current source code.*
