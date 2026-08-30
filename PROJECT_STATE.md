# JValue Project State

This document records the current verified state of JValue and serves as the persistent project-state and AI-agent handoff document.

The repository is a ZeroDepsHack 2026 Track B submission targeting a lightweight, useful JSON toolkit for Java 25 using only the Java standard library at runtime.

All phases through Phase 8 are currently implemented and verified. Phase 9 is the next planned engineering phase.

---

## Project Overview

**Project:** JValue
**Language:** Java 25
**Hackathon:** ZeroDepsHack 2026
**Track:** B — Parsers & Data Formats

JValue is a zero-third-party-runtime-dependency JSON toolkit for Java.

The project exists around a simple gap in the Java platform:

> The JDK provides many general-purpose building blocks but does not provide a complete JSON parsing, tree-model, serialization, file-convenience, or JSON Pointer toolkit.

JValue implements those capabilities using only Java standard-library APIs and handwritten project code.

### Runtime dependency policy

Production code has no third-party runtime dependencies.

Current dependency proof confirms that production classes depend only on `java.base`.

The project does not use:

* Jackson
* Gson
* JUnit
* AssertJ
* Hamcrest
* Guava
* Apache Commons
* external JSON libraries
* other third-party runtime libraries

The build/test process uses JDK-provided tooling and the project's handwritten test infrastructure.

---

## Phase Status

| Phase                                        | Status            | Evidence                                                                                   |
| -------------------------------------------- | ----------------- | ------------------------------------------------------------------------------------------ |
| Phase 1 — Project Skeleton                   | Frozen / complete | Repository structure, build scripts, README, STDLIB.md, `.zero-dep.toml`, dependency proof |
| Phase 2 — JSON Value Model                   | Frozen / complete | `JsonValue` sealed hierarchy and value-model tests                                         |
| Phase 3 — Core Parser                        | Frozen / complete | Recursive-descent parser, parser tests, JSONTestSuite conformance                          |
| Phase 4 — Error Handling & Edge Cases        | Frozen / complete | Position-aware parse errors, malformed-input handling, depth limit, conformance            |
| Phase 5 — Serialization                      | Frozen / complete | Compact/pretty serializer, public serialization APIs, serializer tests                     |
| Phase 6 — File / Convenience APIs            | Frozen / complete | File read/write APIs, charset handling, file API tests                                     |
| Phase 7 — JSON Pointer                       | Frozen / complete | RFC 6901 token parsing, traversal, public lookup APIs, pointer tests                       |
| Phase 8 — Reliability & Hardening            | Frozen / complete | Round-trip, integration, stress, and reliability tests                                     |
| Phase 9 — Performance / Benchmarking Review  | **Next**          | Not started                                                                                |
| Phase 10 — Compliance / Bonus Audit          | Planned           | Not started                                                                                |
| Phase 11 — Final Documentation / Demo Polish | Planned           | Not started                                                                                |

---

## Architecture

The project is intentionally layered and the completed phases should be treated as stable.

### Value Model

`JsonValue` is the sealed root of the JSON tree model.

Concrete JSON values:

* `JsonObject`
* `JsonArray`
* `JsonString`
* `JsonNumber`
* `JsonBoolean`
* `JsonNull`

Values are designed to preserve the project's established equality, immutability, ordering, and numeric-lexeme semantics.

### Parsing

`JsonParser` implements recursive-descent parsing over Java `String` input.

Supporting component:

* `CharSource`

Parse errors are represented by:

* `JsonParseException`

Parse errors include source position information such as:

* character offset;
* line;
* column.

The parser enforces a nesting depth limit of 512.

### Serialization

`JsonSerializer` is package-private/internal.

Public serialization is exposed through `Json`:

* compact serialization;
* pretty serialization;
* `Appendable` output.

Existing `toString()` behavior remains unchanged and is not the JSON serialization contract.

### File / Convenience APIs

File APIs are thin wrappers around the existing parser/serializer and JDK file APIs.

They use JDK facilities such as:

* `Path`
* `Files`
* `Charset`
* `StandardCharsets`
* `BufferedWriter`

The file APIs do not implement a second parsing or serialization policy.

### JSON Pointer

`JsonPointer` implements read-only RFC 6901 lookup over the existing `JsonValue` tree.

It provides:

* pointer compilation;
* RFC 6901 token decoding;
* object traversal;
* array traversal;
* required lookup;
* optional lookup.

Pointer logic is separate from the parser, serializer, and file APIs.

### Testing

The project uses a handwritten JDK-only test harness.

No JUnit or third-party testing framework is used.

The project also includes a JSONTestSuite conformance harness.

---

## Current Public API

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

`JsonSerializer` remains an internal/package-private implementation detail.

---

## Implemented Features

### Parsing

* RFC 8259 JSON parsing from `String`
* JSON null, booleans, numbers, strings, arrays, and objects
* strict JSON number grammar
* lexical number preservation
* string escape handling
* Unicode escape handling
* surrogate pair validation
* rejection of unescaped control characters
* detailed line/column/offset parse errors
* 512-level parser nesting limit
* duplicate object key behavior: last value wins
* rejection of trailing non-whitespace data

### Serialization

* compact JSON serialization
* pretty JSON serialization
* fixed two-space indentation
* no root trailing newline
* JSON string escaping
* Unicode handling
* lone surrogate rejection
* numeric raw-lexeme preservation
* validation of invalid raw numbers
* object iteration-order preservation
* `Appendable` output support

### File APIs

* JSON file reading
* compact JSON file writing
* pretty JSON file writing
* UTF-8 default behavior
* explicit `Charset` overloads
* `IOException` propagation
* integration with the existing parser/serializer
* BOM behavior intentionally remains consistent with the existing parser policy

### JSON Pointer

* RFC 6901 pointer parsing
* empty root pointer
* empty tokens
* `~0` decoding
* `~1` decoding
* malformed escape rejection
* object member traversal
* array index traversal
* strict array-index handling
* out-of-bounds handling
* scalar traversal failure handling
* required and optional lookup semantics
* read-only lookup only

### Reliability / Hardening

* parse → stringify → parse round-trip testing
* cross-feature integration testing
* deep nesting tests
* large collection tests
* large string tests
* numerical edge-case testing
* regression testing
* stress testing of parser/serializer boundaries

---

## Lookup / Error Semantics

### JSON Pointer

For pointer arguments:

* `null` argument → `NullPointerException`
* malformed pointer syntax → `IllegalArgumentException`

For valid but unresolved pointers:

* required lookup → `NoSuchElementException`
* optional lookup → `Optional.empty()`

The empty pointer `""` resolves to the supplied root value unchanged.

For arrays, valid indices are:

* `0`
* non-zero decimal integers without leading zeroes

Invalid/unresolved examples include:

* `01`
* `-1`
* `+1`
* non-numeric tokens
* `-`
* out-of-bounds indices
* values too large for the supported index range

Numeric-looking tokens are treated as ordinary object keys when the current node is a `JsonObject`.

---

## Testing / Verification Status

### Current verified test result

The most recent clean verification produced:

```text
Hand-written tests:
186 passed
0 failed
0 errors
```

JSONTestSuite:

```text
305 passed
0 failed
13 skipped
```

The 13 skipped cases are outside the current parser's `String`-based input boundary and are documented by the project.

### Clean build verification

The following commands have been verified successfully:

```text
build.bat clean
build.bat test
build.bat build
build.bat deps-proof
```

### Dependency proof

`jdeps` currently reports production classes depending only on:

```text
java.base
```

The dependency proof explicitly reports:

```text
Third-party dependencies: NONE
```

JDK modules currently used include standard `java.base` packages such as:

* `java.io`
* `java.lang`
* `java.math`
* `java.nio.charset`
* `java.nio.file`
* `java.util`

---

## Phase 8 Reliability Results

Phase 8 added:

* round-trip tests;
* cross-feature integration tests;
* stress tests.

Current examples include:

* serializer behavior at depth 512;
* serializer behavior beyond normal recursion depth;
* arrays with tens of thousands of elements;
* large object structures;
* million-character strings;
* parse/pointer/stringify integration;
* file read/pointer/stringify integration;
* file write/read/pointer/stringify integration.

An intentionally extreme serializer test observed a `StackOverflowError` at depth 2000 and captures that behavior explicitly rather than treating it as successful arbitrary-depth support.

The project does **not** claim support for arbitrary recursion depth.

The stress suite is intended to characterize realistic behavior and expose regressions, not to guarantee arbitrary-size or arbitrary-depth input handling.

---

## Standard Library Substitutions

Implemented and documented in `STDLIB.md` include:

* JUnit-style testing → handwritten JDK-only test harness and assertions
* external JSON tree model → sealed `JsonValue` hierarchy
* Jackson/Gson parsing → handwritten recursive-descent parser
* Unicode helper libraries → JDK character/string facilities
* external corpus-fetch utilities → JDK HTTP/archive/file APIs
* Jackson/Gson serialization → handwritten serializer using JDK `StringBuilder` / `Appendable`
* external pretty printers → depth-aware JDK-based formatter
* external file utilities → `Files`, `Path`, `Charset`, and related JDK APIs
* external JSON Pointer implementations → immutable JValue `JsonPointer` implementation using JDK types and the existing JSON tree

Do not add artificial substitutions merely to increase the documented count.

---

## Known Non-Blocking Issue

`build.bat` currently emits a stray console line:

```text
'M' is not recognized as an internal or external command
```

before normal script output.

Despite this, the relevant commands:

* build;
* test;
* dependency proof

complete successfully and return successful results.

This is currently treated as a build-script polish issue rather than a functional blocker.

---

## Not Implemented

The following are intentionally outside the current implementation:

* JSON Patch
* JSON mutation APIs
* wildcard / recursive query language
* JSON Schema validation
* POJO binding
* Reader/InputStream convenience parsing
* streaming object-to-JSON serialization
* atomic file writes
* arbitrary-depth guarantees
* full Jackson/Gson feature parity

Do not add these merely to increase feature count.

---

## Remaining Planned Phases

### Phase 9 — Performance / Benchmarking Review

Focus on measuring and reviewing the performance characteristics of the completed toolkit.

Potential areas:

* parser throughput;
* serializer throughput;
* pretty serializer overhead;
* file I/O;
* JSON Pointer lookup;
* allocation-heavy cases;
* moderate large-input behavior.

Performance work should be evidence-driven.

Do not rewrite correct code merely to chase speculative optimizations.

### Phase 10 — Hackathon Compliance / Bonus Audit

Verify final submission requirements and bonus eligibility.

Likely areas:

* dependency proof;
* `STDLIB.md`;
* repository cleanliness;
* required metadata;
* reproducible build / other eligible bonuses if applicable;
* honest feature claims.

### Phase 11 — Final Documentation / Demo Polish

Finalize:

* README;
* demo flow;
* screenshots/video;
* final project explanation;
* write-up preparation;
* final submission artifact review.

---

## Engineering Rules for Future Work

Treat Phases 1–8 as stable.

Before changing an existing feature:

1. identify a concrete bug, missing requirement, or measurable improvement;
2. add or identify evidence;
3. make the smallest justified change;
4. add regression coverage;
5. rerun the relevant test suite;
6. rerun the full verification when the phase requires it.

Do not:

* add third-party runtime dependencies;
* weaken tests;
* remove regression coverage;
* duplicate parser/serializer/file logic;
* redesign frozen APIs without a concrete reason;
* inflate `STDLIB.md` with artificial substitutions;
* claim support that has not been demonstrated;
* expand a phase into later-phase functionality.

---

## Current State Summary

```text
Phase 1  ✅ Frozen
Phase 2  ✅ Frozen
Phase 3  ✅ Frozen
Phase 4  ✅ Frozen
Phase 5  ✅ Frozen
Phase 6  ✅ Frozen
Phase 7  ✅ Frozen
Phase 8  ✅ Frozen

Phase 9  → Next
Phase 10 → Planned
Phase 11 → Planned
```

The current repository is a verified Java 25, zero-third-party-runtime-dependency JSON toolkit with parsing, serialization, file convenience APIs, RFC 6901 read-only JSON Pointer support, and a 186-test reliability suite.
