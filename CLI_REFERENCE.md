# JValue CLI — Cheatsheet

## One-time Setup
```powershell
$env:JAVA_HOME = "D:\Downloads\jdk-25.0.4_windows-x64_bin\jdk-25.0.4"
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
cd d:\JValue\JValue
.\build.bat build
New-Item -Force -ItemType Directory cli\classes | Out-Null
javac --release 25 -cp "build\classes" -d "cli\classes" "cli\JValueCli.java"
```

---

## Commands  (`.\jv.bat <cmd> [args]`)

| Command | Short | Args | What it does |
|---|---|---|---|
| `help` | `h` | — | Show all commands |
| `version` | `v` | — | Library version |
| `validate` | `val` | `<input>` | Check validity — shows line/col on error |
| `pretty` | `p` | `<input>` | Pretty-print with 2-space indent |
| `compact` | `c` | `<input>` | Minify JSON |
| `get` | `g` | `<input> <ptr>` | JSON Pointer lookup  e.g. `/users/0/name` |
| `inspect` | `i` | `<input>` | Full type tree of every field |
| `tomap` | `m` | `<input>` | JSON → Java Map/List view + back to JSON |
| `read` | `r` | `<file>` | Read file + pretty-print |
| `write` | `w` | `<input> <file>` | Write pretty JSON to file |
| `build` | `b` | `k=v k=v ...` | Java object → JSON from key=value pairs |
| `array` | `arr` | `v v v ...` | Java array → JSON from values |
| `numinfo` | `n` | `<number>` | raw / int / long / double / BigDecimal |
| `stress` | `st` | `depth\|dupkey` | Depth-limit (512) and duplicate key demo |
| `pointer` | `ptr` | `<pointer>` | Compile pointer + show decoded tokens |
| `roundtrip` | `rt` | `<input>` | Parse → compact → re-parse → verify |

> `<input>` = file path **or** raw JSON string

---

## Quick Examples

```powershell
.\jv.bat pretty        demo\data\users.json
.\jv.bat get           demo\data\users.json  /users/0/name
.\jv.bat inspect       demo\data\valid_object.json
.\jv.bat tomap         demo\data\users.json
.\jv.bat build         name=Ada age=36 active=true score=99.5
.\jv.bat array         10 Ada true 3.14 null
.\jv.bat numinfo       9007199254740991
.\jv.bat stress        depth
.\jv.bat stress        dupkey
.\jv.bat pointer       /users/0/address/city
.\jv.bat validate      demo\data\edge_strings.json
.\jv.bat roundtrip     demo\data\users.json
.\jv.bat write         demo\data\users.json  output.json
```

---

## Run Manual Test Suite (185 checks)
```powershell
New-Item -Force -ItemType Directory demo\classes | Out-Null
javac --release 25 -cp "build\classes" -d "demo\classes" "demo\ManualDemo.java"
java  -cp "build\classes;demo\classes" ManualDemo
```
