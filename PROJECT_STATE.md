# JValue — Project State

## Overview

JValue is a lightweight JSON toolkit for Java 25 built entirely on the Java standard library, with **zero third-party runtime dependencies**.

It provides a complete tree-based workflow for working with JSON documents:

```text
JSON text / file
      ↓
   parsing
      ↓
 JsonValue tree
      ↓
pointer lookup / inspection
      ↓
serialization
      ↓
JSON text / file
```

The project is designed for developers who want practical JSON parsing and document manipulation without bringing a third-party JSON runtime into a project.

---

## Current Implementation Status

The current implementation includes:

### JSON Value Model

* `JsonValue` sealed root type.
* `JsonObject`
* `JsonArray`
* `JsonString`
* `JsonNumber`
* `JsonBoolean`
* `JsonNull`
* immutable value semantics and predictable object iteration order.

### Parsing

* RFC 8259 JSON parsing from Java `String` input.
* Recursive-descent parser.
* Strict JSON number grammar.
* Preservation of the original numeric lexeme.
* JSON string escape handling.
* Unicode escape handling.
* Surrogate-pair validation.
* Rejection of unescaped control characters.
* Rejection of trailing non-whitespace data.
* Duplicate-object-key policy: last value wins.
* Detailed parse diagnostics with line, column, and character offset.
* Maximum parser nesting depth of 512.

### Serialization

* Compact JSON serialization.
* Pretty JSON serialization.
* Fixed two-space indentation.
* No trailing root newline.
* Correct JSON string escaping.
* Unicode handling.
* Lone-surrogate rejection.
* Raw numeric lexeme preservation.
* Validation of invalid raw numeric values.
* Object iteration-order preservation.
* `Appendable` output support.

### File Convenience APIs

* JSON file reading.
* Compact JSON file writing.
* Pretty JSON file writing.
* UTF-8 default behavior.
* Explicit `Charset` overloads.
* `IOException` propagation.
* File APIs reuse the existing parser and serializer rather than maintaining separate implementations.

### JSON Pointer

* Read-only RFC 6901 JSON Pointer support.
* Compiled immutable pointers.
* `~0` and `~1` token decoding.
* Empty/root pointer.
* Object member traversal.
* Array index traversal.
* Required lookup.
* Optional lookup.
* Explicit distinction between malformed pointers and valid-but-unresolved pointers.

### Command-Line Interface (CLI)

* Native wrapper scripts for Windows (`jv.bat`) and Unix/Linux/macOS (`jv.sh`).
* `validate`: Parse a file/string and report exact line/column of errors.
* `pretty` / `compact`: Format JSON.
* `get`: RFC 6901 JSON Pointer lookup.
* `inspect`: Tree-based type inspection.
* `tomap`: Java type view.
* `read` / `write`: File I/O.
* `build` / `array`: Jackson-free JSON object/array builder from arguments.
* `numinfo`: Numeric lexeme inspection.

### Reliability / Hardening

* Parse → serialize → parse round-trip tests.
* Cross-feature integration tests.
* Deep-nesting tests.
* Large-collection tests.
* Large-string tests.
* Numeric edge-case tests.
* Serialization and pointer regression tests.
* Stress testing of parser and serializer boundaries.

---

## Public API

```java
Json.parse(String json)

Json.stringify(JsonValue value)

Json.stringifyPretty(JsonValue value)

Json.write(JsonValue value, Appendable out)

Json.writePretty(JsonValue value, Appendable out)

Json.read(Path path)

Json.read(Path path, Charset charset)

Json.writeFile(JsonValue value, Path path)

Json.writeFile(JsonValue value, Path path, Charset charset)

Json.writePrettyFile(JsonValue value, Path path)

Json.writePrettyFile(JsonValue value, Path path, Charset charset)

JsonPointer.compile(String pointer)

JsonPointer.query(JsonValue root)

JsonPointer.queryOptional(JsonValue root)

Json.pointer(JsonValue root, String pointer)

Json.pointerOptional(JsonValue root, String pointer)
```

`JsonSerializer` is an internal/package-private implementation detail.

Existing `toString()` behavior is intentionally unchanged; explicit serialization APIs define JSON output.

---

## Error and Lookup Semantics

### Parsing

Malformed JSON produces `JsonParseException` with source-location information.

### JSON Pointer

* `null` arguments → `NullPointerException`
* malformed pointer syntax → `IllegalArgumentException`
* valid but unresolved required lookup → `NoSuchElementException`
* valid but unresolved optional lookup → `Optional.empty()`

The empty pointer `""` returns the root value unchanged.

Array indices follow strict decimal-index rules without leading zeroes except for `0`.

---

## Verification Status

The final implementation has been repeatedly verified using a clean generated-build state.

### Handwritten test suite

```text
186 passed
0 failed
0 errors
```

### JSONTestSuite

```text
305 passed
0 failed
13 skipped
```

The skipped cases concern byte-level encoding scenarios outside the current parser API boundary, which accepts Java `String` input rather than raw byte streams.

### Build

The following commands succeed:

```text
build.bat clean
build.bat test
build.bat build
build.bat deps-proof
```

### Dependency proof

`jdeps` confirms that production classes depend only on:

```text
java.base
```

The project therefore has:

```text
Third-party runtime dependencies: NONE
```

---

## Performance Review

Performance was measured after functional correctness was established.

The benchmark harness is JDK-only and uses warmup plus repeated measurements with `System.nanoTime()`.

Representative workloads include:

* small JSON documents;
* medium documents;
* approximately 1 MB documents;
* compact serialization;
* pretty serialization;
* JSON Pointer lookup;
* file I/O;
* deep nesting;
* large strings;
* round-trip operations.

A candidate optimization replaced character-by-character pretty-print indentation with `String.repeat()`.

On the primary development machine, the replacement was measured to be slower for the tested workload, so it was reverted.

This is intentionally documented as a benchmark result rather than treated as a universal statement about JVM performance.

The current implementation keeps the measured faster approach.

---

## Reliability Notes

The reliability suite verifies behavior at realistic large and deep inputs.

The serializer was also tested beyond the normal supported recursion range. Extremely deep recursive serialization can eventually exhaust the JVM stack; the project does not claim arbitrary recursion depth.

The project therefore distinguishes between:

* supported and tested operating ranges;
* experimentally characterized stress limits;
* unsupported arbitrary-size/arbitrary-depth guarantees.

---

## Standard Library Approach

JValue intentionally avoids third-party JSON/runtime libraries and implements its required functionality using JDK APIs and project code.

Examples include:

* JSON tree representation → sealed Java value hierarchy;
* JSON parsing → handwritten recursive-descent parser;
* JSON serialization → handwritten serializer using `StringBuilder` / `Appendable`;
* pretty printing → depth-aware traversal using JDK string/output facilities;
* file utilities → `java.nio.file` and related JDK APIs;
* JSON Pointer → compact immutable pointer parser and tree traversal;
* testing infrastructure → handwritten JDK-only test harness.

See `STDLIB.md` for the detailed substitution ledger and associated trade-offs.

---

## Known Limitations

JValue intentionally does not attempt full feature parity with large general-purpose JSON frameworks.

Not implemented:

* JSON Patch;
* JSON mutation APIs;
* wildcard or recursive query languages;
* JSON Schema validation;
* POJO binding;
* arbitrary-depth guarantees;
* arbitrary-size guarantees;
* streaming object-to-JSON serialization;
* atomic file-write helpers;
* general `Reader` / `InputStream` convenience parsing APIs.

These omissions are deliberate scope choices rather than hidden dependencies.

---

## Build and Runtime Model

The project uses the JDK directly.

Build tooling:

```text
javac
java
jdeps
```

No third-party runtime dependency is required.

The primary project build/test interface is:

```text
build.bat build
build.bat test
build.bat deps-proof
```

The primary user-facing tool interface is the CLI wrapper:

```text
./jv.sh help
./jv.sh validate <file>
```

---

## Current Release State

The implementation is functionally complete for its current intended scope and has passed:

* the handwritten regression suite;
* JSONTestSuite conformance checks;
* clean build verification;
* dependency verification;
* reliability/stress testing;
* performance review.

Remaining work is limited to final submission presentation and documentation polish.
