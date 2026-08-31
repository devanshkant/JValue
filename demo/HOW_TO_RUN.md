# JValue Manual Demo — How to Run

## What this tests
- Feature 1: Json.parse — 44 checks (all types, escapes, unicode, error cases)
- Feature 2: Value Model — 42 checks (JsonObject, JsonArray, JsonNumber, factories, depth limit)
- Feature 3: Json.stringify compact — 17 checks (all types, escaping, raw lexeme, insertion order)
- Feature 4: Json.stringifyPretty — 8 checks (indentation, no trailing newline, Appendable)
- Feature 5: File API — 22 checks (read/write/pretty, charsets, round-trip, error paths)
- Feature 6: JsonPointer RFC 6901 — 25 checks (navigation, ~0/~1 escapes, optional, compiled, errors)
- Feature 7: Error Handling — 8 checks (line/column/offset, ClassCastException, NoSuchElement)
- Feature 8: Round-Trip — 16 checks (all types parse→stringify→parse)
- Feature 9: Build from Java — 9 checks (Jackson-free object building, file write)

TOTAL: 185 checks

---

## Prerequisites

Java 25 must be on PATH. If you get "java 23" from `java --version`, run this first in each terminal:

**Windows (PowerShell)**:
```powershell
$env:JAVA_HOME = "D:\Downloads\jdk-25.0.4_windows-x64_bin\jdk-25.0.4"
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
java --version   # Should show: java 25.0.4
```

**Linux / macOS (Bash)**:
```bash
export JAVA_HOME="/path/to/jdk-25.0.4"
export PATH="$JAVA_HOME/bin:$PATH"
java --version   # Should show: java 25.0.4
```

---

## Step 1 — Build the main library

**Windows**:
```powershell
cd d:\JValue\JValue
.\build.bat build
```

**Linux / macOS**:
```bash
cd /path/to/JValue
./build.sh build
```

Expected:
```
[JValue] Compiling main sources...
[OK] Build complete.
```

---

## Step 2 — Compile the demo

**Windows**:
```powershell
New-Item -ItemType Directory -Force demo\classes
javac --release 25 -cp "build\classes" -d "demo\classes" "demo\ManualDemo.java"
```

**Linux / macOS**:
```bash
mkdir -p demo/classes
javac --release 25 -cp "build/classes" -d "demo/classes" "demo/ManualDemo.java"
```

Expected: No errors, silent success.

---

## Step 3 — Run the demo

**Windows**:
```powershell
java -cp "build\classes;demo\classes" ManualDemo
```

**Linux / macOS**:
```bash
java -cp "build/classes:demo/classes" ManualDemo
```

Expected final lines:
```
══════════════════════════════════════════
  TOTAL: 185 passed, 0 failed
══════════════════════════════════════════
```

---

## One-liner (all 3 steps together)

**Windows**:
```powershell
$env:JAVA_HOME = "D:\Downloads\jdk-25.0.4_windows-x64_bin\jdk-25.0.4"; $env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH; .\build.bat build; New-Item -Force -ItemType Directory demo\classes | Out-Null; javac --release 25 -cp "build\classes" -d "demo\classes" "demo\ManualDemo.java"; java -cp "build\classes;demo\classes" ManualDemo
```

**Linux / macOS**:
```bash
export JAVA_HOME="/path/to/jdk-25.0.4"; export PATH="$JAVA_HOME/bin:$PATH"; ./build.sh build; mkdir -p demo/classes; javac --release 25 -cp "build/classes" -d "demo/classes" "demo/ManualDemo.java"; java -cp "build/classes:demo/classes" ManualDemo
```

---

## Output files created during the run

After running, check these in `demo\output\`:

| File | What it is |
|---|---|
| `compact_out.json`  | Compact serialization of valid_object.json |
| `pretty_out.json`   | Pretty serialization of valid_object.json |
| `roundtrip.json`    | Round-tripped users.json |
| `built_user.json`   | Java-built user object written to file |

---

## Run the official automated test suite (186 tests + 305 conformance)

**Windows**:
```powershell
.\build.bat test
```

**Linux / macOS**:
```bash
./build.sh test
```
