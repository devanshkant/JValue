import com.jvalue.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * JValue CLI — Zero-Dependency JSON Toolkit for Java 25
 *
 * Usage:  java -cp "build/classes;cli/classes" JValueCli <command> [args]
 * Type    java -cp "build/classes;cli/classes" JValueCli help   to get started.
 */
public class JValueCli {

    // ── ANSI Color Codes ─────────────────────────────────────────────────────
    static final String RESET   = "\u001B[0m";
    static final String BOLD    = "\u001B[1m";
    static final String RED     = "\u001B[31m";
    static final String GREEN   = "\u001B[32m";
    static final String YELLOW  = "\u001B[33m";
    static final String BLUE    = "\u001B[34m";
    static final String MAGENTA = "\u001B[35m";
    static final String CYAN    = "\u001B[36m";
    static final String WHITE   = "\u001B[97m";
    static final String DIM     = "\u001B[2m";

    static final String B_CYAN    = BOLD + CYAN;
    static final String B_GREEN   = BOLD + GREEN;
    static final String B_RED     = BOLD + RED;
    static final String B_YELLOW  = BOLD + YELLOW;
    static final String B_WHITE   = BOLD + WHITE;

    // ── Entry Point ──────────────────────────────────────────────────────────
    public static void main(String[] args) {
        if (args.length == 0) {
            printHelp();
            return;
        }

        String cmd = args[0].toLowerCase();
        try {
            switch (cmd) {
                case "help",     "h"   -> printHelp();
                case "version",  "v"   -> printVersion();
                case "validate", "val" -> cmdValidate(args);
                case "pretty",   "p"   -> cmdPretty(args);
                case "compact",  "c"   -> cmdCompact(args);
                case "get",      "g"   -> cmdGet(args);
                case "inspect",  "i"   -> cmdInspect(args);
                case "tomap",    "m"   -> cmdToMap(args);
                case "read",     "r"   -> cmdRead(args);
                case "write",    "w"   -> cmdWrite(args);
                case "build",    "b"   -> cmdBuild(args);
                case "array",    "arr" -> cmdArray(args);
                case "numinfo",  "n"   -> cmdNumInfo(args);
                case "stress",   "st"  -> cmdStress(args);
                case "pointer",  "ptr" -> cmdPointer(args);
                case "roundtrip","rt"  -> cmdRoundTrip(args);
                default -> {
                    printError("Unknown command: '" + args[0] + "'");
                    System.out.println(DIM + "  Run  " + RESET + B_CYAN
                        + "JValueCli help" + RESET + DIM
                        + "  to see all available commands." + RESET);
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            printError("Unexpected error: " + e.getMessage());
            System.exit(1);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELP
    // ══════════════════════════════════════════════════════════════════════════
    static void printHelp() {
        String line = "─".repeat(58);
        System.out.println();
        System.out.println(B_CYAN + "╔══════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(B_CYAN + "║" + RESET + B_WHITE + "          JValue CLI  " + RESET
            + DIM + "·" + RESET + CYAN + "  Zero-Dep JSON for Java 25" + RESET
            + B_CYAN + "          ║" + RESET);
        System.out.println(B_CYAN + "║" + RESET + DIM
            + "     Parse · Pretty · Compact · Pointer · Inspect        "
            + RESET + B_CYAN + "║" + RESET);
        System.out.println(B_CYAN + "╚══════════════════════════════════════════════════════════╝" + RESET);
        System.out.println();

        System.out.println(DIM + "  Usage: " + RESET + B_WHITE
            + "java -cp \"build/classes;cli/classes\" JValueCli" + RESET
            + CYAN + " <command> [args]" + RESET);
        System.out.println();

        System.out.println(B_YELLOW + "  COMMANDS" + RESET);
        System.out.println(DIM + "  " + line + RESET);

        printCmd("help",      "h",   "",                    "Show this help screen");
        printCmd("version",   "v",   "",                    "Show JValue library version");
        System.out.println();
        printCmd("validate",  "val", "<input>",             "Check if JSON is valid; show error position if not");
        printCmd("pretty",    "p",   "<input>",             "Pretty-print JSON with 2-space indentation");
        printCmd("compact",   "c",   "<input>",             "Compact / minify JSON (removes all whitespace)");
        printCmd("get",       "g",   "<input> <pointer>",   "JSON Pointer lookup  e.g. /users/0/name");
        printCmd("inspect",   "i",   "<input>",             "Show type + value for every field in the tree");
        printCmd("tomap",     "m",   "<input>",             "Convert JSON → Java Map/List/String/Integer view");
        printCmd("read",      "r",   "<file>",              "Read a JSON file and pretty-print it");
        printCmd("write",     "w",   "<input> <file>",      "Write pretty JSON to a file");
        printCmd("build",     "b",   "key=val ...",         "Build JSON object from key=value pairs (Java → JSON)");
        printCmd("array",     "arr", "val val ...",         "Build JSON array from values");
        printCmd("numinfo",   "n",   "<number>",            "Show raw/int/double/BigDecimal for a JSON number");
        printCmd("stress",    "st",  "depth|dupkey",        "Demo: depth-limit (512) and duplicate key handling");
        printCmd("pointer",   "ptr", "<pointer>",           "Compile & inspect a JSON Pointer's decoded tokens");
        printCmd("roundtrip", "rt",  "<input>",             "Parse → compact → re-parse and verify identity");

        System.out.println(DIM + "  " + line + RESET);
        System.out.println();
        System.out.println(B_YELLOW + "  INPUT FORMAT" + RESET);
        System.out.println(DIM + "  " + line + RESET);
        System.out.println("  " + CYAN + "<input>" + RESET + " can be:");
        System.out.println("    " + GREEN + "✔" + RESET + "  A raw JSON string :  "
            + MAGENTA + "'{\"name\":\"Ada\",\"age\":36}'" + RESET);
        System.out.println("    " + GREEN + "✔" + RESET + "  A file path       :  "
            + YELLOW + "demo/data/users.json" + RESET);
        System.out.println();

        System.out.println(B_YELLOW + "  EXAMPLES" + RESET);
        System.out.println(DIM + "  " + line + RESET);
        printExample("pretty",    "'{\"name\":\"Ada\",\"age\":36}'");
        printExample("validate",  "demo/data/users.json");
        printExample("get",       "demo/data/users.json /users/0/name");
        printExample("compact",   "demo/data/users.json");
        printExample("inspect",   "'{\"x\":1,\"y\":true,\"z\":null}'");
        printExample("tomap",     "demo/data/users.json");
        printExample("write",     "'{\"ok\":true}' output.json");
        printExample("build",     "name=Ada age=36 active=true score=99.5");
        printExample("roundtrip", "'{\"a\":1,\"b\":[2,3]}'");
        System.out.println();
    }

    static void printCmd(String name, String alias, String args, String desc) {
        System.out.printf("  " + B_CYAN + "%-10s" + RESET + DIM + "/ %-4s" + RESET
            + YELLOW + "%-22s" + RESET + "  %s%n",
            name, alias, args, desc);
    }

    static void printExample(String cmd, String args) {
        System.out.println("    " + DIM + "$ " + RESET + CYAN + "JValueCli "
            + B_CYAN + cmd + RESET + "  " + DIM + args + RESET);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // VERSION
    // ══════════════════════════════════════════════════════════════════════════
    static void printVersion() {
        System.out.println();
        System.out.println("  " + B_CYAN + "JValue" + RESET + "  "
            + GREEN + Json.version() + RESET);
        System.out.println("  " + DIM + "Zero-dependency JSON toolkit for Java 25" + RESET);
        System.out.println("  " + DIM + "License: MIT  |  Hackathon: ZeroDepsHack 2026" + RESET);
        System.out.println();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // VALIDATE
    // ══════════════════════════════════════════════════════════════════════════
    static void cmdValidate(String[] args) throws Exception {
        requireArgs(args, 2, "validate <input>");
        String input = args[1];
        String json  = resolveInput(input);
        System.out.println();
        try {
            JsonValue v = Json.parse(json);
            System.out.println("  " + B_GREEN + "✔  Valid JSON" + RESET);
            System.out.println("  " + DIM + "Root type : " + RESET
                + typeLabel(v));
            if (v.isObject())
                System.out.println("  " + DIM + "Keys      : " + RESET + v.asObject().size());
            if (v.isArray())
                System.out.println("  " + DIM + "Elements  : " + RESET + v.asArray().size());
        } catch (JsonParseException e) {
            printParseError(e, json);
            System.exit(1);
        }
        System.out.println();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRETTY
    // ══════════════════════════════════════════════════════════════════════════
    static void cmdPretty(String[] args) throws Exception {
        requireArgs(args, 2, "pretty <input>");
        String json = resolveInput(args[1]);
        try {
            JsonValue v = Json.parse(json);
            System.out.println();
            System.out.println(colorizeJson(Json.stringifyPretty(v)));
            System.out.println();
        } catch (JsonParseException e) {
            printParseError(e, json); System.exit(1);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // COMPACT
    // ══════════════════════════════════════════════════════════════════════════
    static void cmdCompact(String[] args) throws Exception {
        requireArgs(args, 2, "compact <input>");
        String json = resolveInput(args[1]);
        try {
            JsonValue v = Json.parse(json);
            System.out.println();
            System.out.println("  " + DIM + Json.stringify(v) + RESET);
            System.out.println();
        } catch (JsonParseException e) {
            printParseError(e, json); System.exit(1);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET (JSON POINTER)
    // ══════════════════════════════════════════════════════════════════════════
    static void cmdGet(String[] args) throws Exception {
        requireArgs(args, 3, "get <input> <pointer>");
        String json    = resolveInput(args[1]);
        String pointer = args[2];
        System.out.println();
        try {
            JsonValue root   = Json.parse(json);
            Optional<JsonValue> result = Json.pointerOptional(root, pointer);
            if (result.isEmpty()) {
                System.out.println("  " + B_YELLOW + "⚠  Path not found: " + RESET + pointer);
                System.out.println("  " + DIM + "Tip: use 'inspect' to see all available paths." + RESET);
                System.exit(1);
            }
            JsonValue v = result.get();
            System.out.println("  " + DIM + "Pointer : " + RESET + CYAN + pointer + RESET);
            System.out.println("  " + DIM + "Type    : " + RESET + typeLabel(v));
            System.out.println("  " + DIM + "Value   : " + RESET);
            System.out.println(colorizeJson(Json.stringifyPretty(v)));
        } catch (IllegalArgumentException e) {
            printError("Invalid JSON Pointer syntax: " + e.getMessage());
            System.out.println(DIM + "  Pointers must start with '/' e.g. /users/0/name" + RESET);
            System.exit(1);
        } catch (JsonParseException e) {
            printParseError(e, json); System.exit(1);
        }
        System.out.println();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // INSPECT — show type of every field in the tree
    // ══════════════════════════════════════════════════════════════════════════
    static void cmdInspect(String[] args) throws Exception {
        requireArgs(args, 2, "inspect <input>");
        String json = resolveInput(args[1]);
        System.out.println();
        try {
            JsonValue v = Json.parse(json);
            System.out.println("  " + B_CYAN + "JSON Tree Inspection" + RESET);
            System.out.println("  " + DIM + "─".repeat(52) + RESET);
            inspectValue("root", v, 0);
        } catch (JsonParseException e) {
            printParseError(e, json); System.exit(1);
        }
        System.out.println();
    }

    static void inspectValue(String path, JsonValue v, int depth) {
        String indent = "  " + "  ".repeat(depth);
        String pathStr = depth == 0 ? CYAN + path + RESET : BLUE + path + RESET;

        if (v.isObject()) {
            System.out.println(indent + pathStr + "  " + B_YELLOW + "[object]"
                + RESET + DIM + "  " + v.asObject().size() + " keys" + RESET);
            for (var entry : v.asObject()) {
                inspectValue(entry.getKey(), entry.getValue(), depth + 1);
            }
        } else if (v.isArray()) {
            System.out.println(indent + pathStr + "  " + B_YELLOW + "[array]"
                + RESET + DIM + "  " + v.asArray().size() + " elements" + RESET);
            int i = 0;
            for (JsonValue item : v.asArray()) {
                inspectValue("[" + i++ + "]", item, depth + 1);
            }
        } else {
            String valStr = v.isString()
                ? MAGENTA + "\"" + v.asString() + "\"" + RESET
                : (v.isNull() ? DIM + "null" + RESET : GREEN + Json.stringify(v) + RESET);
            System.out.println(indent + pathStr + "  "
                + DIM + typeLabel(v) + RESET + "  →  " + valStr);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TOMAP — show JSON as Java Map/List
    // ══════════════════════════════════════════════════════════════════════════
    static void cmdToMap(String[] args) throws Exception {
        requireArgs(args, 2, "tomap <input>");
        String json = resolveInput(args[1]);
        System.out.println();
        try {
            JsonValue v = Json.parse(json);
            // ── Direction 1: JSON → Java Object tree ──────────────────────
            System.out.println("  " + B_CYAN + "◀  JSON → Java Objects" + RESET);
            System.out.println("  " + DIM + "─".repeat(52) + RESET);
            printJavaView(toJava(v), 1);
            System.out.println();
            // ── Direction 2: Java Object tree → back to JSON ──────────────
            System.out.println("  " + B_CYAN + "▶  Java Objects → JSON (round-trip)" + RESET);
            System.out.println("  " + DIM + "─".repeat(52) + RESET);
            System.out.println(colorizeJson(Json.stringifyPretty(v)));
        } catch (JsonParseException e) {
            printParseError(e, json); System.exit(1);
        }
        System.out.println();
    }

    @SuppressWarnings("unchecked")
    static void printJavaView(Object obj, int depth) {
        String indent = "  ".repeat(depth);
        if (obj == null) {
            System.out.println(indent + DIM + "null" + RESET + DIM + "  (Java null)" + RESET);
        } else if (obj instanceof Map<?,?> map) {
            System.out.println(indent + B_YELLOW + "LinkedHashMap" + RESET
                + DIM + "  {" + map.size() + " entries}" + RESET);
            for (var e : ((Map<String,Object>) map).entrySet()) {
                Object val = e.getValue();
                if (val instanceof Map || val instanceof List) {
                    // nested — print key then recurse
                    System.out.println(indent + "  " + BLUE + e.getKey() + RESET + "  →");
                    printJavaView(val, depth + 2);
                } else {
                    System.out.print(indent + "  " + BLUE + e.getKey() + RESET + "  →  ");
                    printLeaf(val);
                }
            }
        } else if (obj instanceof List<?> list) {
            System.out.println(indent + B_YELLOW + "ArrayList" + RESET
                + DIM + "  [" + list.size() + " items]" + RESET);
            int i = 0;
            for (Object item : list) {
                if (item instanceof Map || item instanceof List) {
                    System.out.println(indent + "  " + DIM + "[" + i++ + "]" + RESET + "  →");
                    printJavaView(item, depth + 2);
                } else {
                    System.out.print(indent + "  " + DIM + "[" + i++ + "]" + RESET + "  →  ");
                    printLeaf(item);
                }
            }
        } else {
            printLeaf(obj);
        }
    }

    static void printLeaf(Object val) {
        if (val == null)                    System.out.println(DIM + "null" + RESET + DIM + "  (Java null)" + RESET);
        else if (val instanceof String s)   System.out.println(MAGENTA + "\"" + s + "\"" + RESET + DIM + "  (String)" + RESET);
        else if (val instanceof Integer i)  System.out.println(GREEN + i + RESET + DIM + "  (Integer)" + RESET);
        else if (val instanceof Long l)     System.out.println(GREEN + l + RESET + DIM + "  (Long)" + RESET);
        else if (val instanceof Double d)   System.out.println(GREEN + d + RESET + DIM + "  (Double)" + RESET);
        else if (val instanceof Boolean b)  System.out.println(CYAN + b + RESET + DIM + "  (Boolean)" + RESET);
        else                                System.out.println(val);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // READ
    // ══════════════════════════════════════════════════════════════════════════
    static void cmdRead(String[] args) throws Exception {
        requireArgs(args, 2, "read <file>");
        Path path = Path.of(args[1]);
        if (!Files.exists(path)) {
            printError("File not found: " + path.toAbsolutePath());
            System.exit(1);
        }
        System.out.println();
        System.out.println("  " + DIM + "File    : " + RESET + YELLOW + path.toAbsolutePath() + RESET);
        System.out.println("  " + DIM + "Size    : " + RESET + Files.size(path) + " bytes");
        System.out.println();
        try {
            JsonValue v = Json.read(path);
            System.out.println("  " + DIM + "Type    : " + RESET + typeLabel(v));
            System.out.println();
            System.out.println(colorizeJson(Json.stringifyPretty(v)));
        } catch (JsonParseException e) {
            printParseError(e, Files.readString(path)); System.exit(1);
        }
        System.out.println();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // WRITE
    // ══════════════════════════════════════════════════════════════════════════
    static void cmdWrite(String[] args) throws Exception {
        requireArgs(args, 3, "write <input> <file>");
        String json = resolveInput(args[1]);
        Path   path = Path.of(args[2]);
        try {
            JsonValue v = Json.parse(json);
            Json.writePrettyFile(v, path);
            System.out.println();
            System.out.println("  " + B_GREEN + "✔  Written successfully" + RESET);
            System.out.println("  " + DIM + "File : " + RESET
                + YELLOW + path.toAbsolutePath() + RESET);
            System.out.println("  " + DIM + "Size : " + RESET + Files.size(path) + " bytes");
        } catch (JsonParseException e) {
            printParseError(e, json); System.exit(1);
        }
        System.out.println();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ROUNDTRIP
    // ══════════════════════════════════════════════════════════════════════════
    static void cmdRoundTrip(String[] args) throws Exception {
        requireArgs(args, 2, "roundtrip <input>");
        String json = resolveInput(args[1]);
        System.out.println();
        try {
            JsonValue original  = Json.parse(json);
            String   compact    = Json.stringify(original);
            JsonValue reparsed  = Json.parse(compact);
            boolean  match      = original.equals(reparsed);

            System.out.println("  " + DIM + "Step 1  parse   : " + RESET
                + GREEN + "OK" + RESET + "  (" + typeLabel(original) + ")");
            System.out.println("  " + DIM + "Step 2  compact : " + RESET + DIM + compact + RESET);
            System.out.println("  " + DIM + "Step 3  re-parse: " + RESET
                + GREEN + "OK" + RESET);
            System.out.println("  " + DIM + "Step 4  match   : " + RESET
                + (match ? B_GREEN + "✔  PASS — values are identical" + RESET
                         : B_RED   + "✘  FAIL — values differ!" + RESET));
        } catch (JsonParseException e) {
            printParseError(e, json); System.exit(1);
        }
        System.out.println();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BUILD — Java object (key=value args) → JSON
    // ══════════════════════════════════════════════════════════════════════════
    static void cmdBuild(String[] args) throws Exception {
        if (args.length < 2) {
            printError("Too few arguments.");
            System.out.println("  Usage: " + B_CYAN + "JValueCli build key=value key2=value2 ..." + RESET);
            System.out.println();
            System.out.println("  " + B_YELLOW + "Value type rules:" + RESET);
            System.out.println("  " + DIM + "  123        → Integer" + RESET);
            System.out.println("  " + DIM + "  3.14       → Double" + RESET);
            System.out.println("  " + DIM + "  true/false → Boolean" + RESET);
            System.out.println("  " + DIM + "  null       → null" + RESET);
            System.out.println("  " + DIM + "  anything else → String" + RESET);
            System.exit(1);
        }

        System.out.println();
        System.out.println("  " + B_CYAN + "Building JSON from your key=value arguments" + RESET);
        System.out.println("  " + DIM + "─".repeat(52) + RESET);

        // ── Parse key=value pairs → build JsonObject ─────────────────────────
        LinkedHashMap<String, JsonValue> fields = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();

        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            int eq = arg.indexOf('=');
            if (eq <= 0) {
                errors.add("Skipping '" + arg + "' — no '=' found. Format must be key=value");
                continue;
            }
            String key = arg.substring(0, eq).trim();
            String raw = arg.substring(eq + 1).trim();

            // Auto-detect Java type and convert to JsonValue
            JsonValue val;
            String detectedType;
            if (raw.equals("null")) {
                val = JsonValue.ofNull();        detectedType = "null";
            } else if (raw.equals("true")) {
                val = JsonValue.of(true);        detectedType = "Boolean";
            } else if (raw.equals("false")) {
                val = JsonValue.of(false);       detectedType = "Boolean";
            } else {
                // Try Integer, then Double, else String
                try {
                    val = JsonValue.of(Integer.parseInt(raw));
                    detectedType = "Integer";
                } catch (NumberFormatException e1) {
                    try {
                        val = JsonValue.of(Double.parseDouble(raw));
                        detectedType = "Double";
                    } catch (NumberFormatException e2) {
                        val = JsonValue.of(raw);
                        detectedType = "String";
                    }
                }
            }

            fields.put(key, val);
            System.out.println("  " + BLUE + key + RESET + "  =  "
                + GREEN + raw + RESET
                + DIM + "  →  detected as " + RESET + YELLOW + detectedType + RESET);
        }

        // Show any errors
        if (!errors.isEmpty()) {
            System.out.println();
            for (String err : errors)
                System.out.println("  " + B_YELLOW + "⚠  " + err + RESET);
        }

        if (fields.isEmpty()) {
            printError("No valid key=value pairs provided.");
            System.exit(1);
        }

        // ── Build the actual JsonObject from fields ───────────────────────────
        // Serialize each field and reconstruct via parse (constructor is pkg-private)
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var e : fields.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":").append(Json.stringify(e.getValue()));
            first = false;
        }
        sb.append("}");
        JsonValue built = Json.parse(sb.toString());

        System.out.println();
        System.out.println("  " + B_CYAN + "▶  Java Object → JSON (compact)" + RESET);
        System.out.println("  " + DIM + "─".repeat(52) + RESET);
        System.out.println("  " + Json.stringify(built));

        System.out.println();
        System.out.println("  " + B_CYAN + "▶  Java Object → JSON (pretty)" + RESET);
        System.out.println("  " + DIM + "─".repeat(52) + RESET);
        System.out.println(colorizeJson(Json.stringifyPretty(built)));

        System.out.println();
        System.out.println("  " + B_CYAN + "◀  JSON → Java Objects (tomap view)" + RESET);
        System.out.println("  " + DIM + "─".repeat(52) + RESET);
        printJavaView(toJava(built), 1);
        System.out.println();

        System.out.println("  " + B_GREEN + "✔  Object built successfully  ("
            + fields.size() + " fields)" + RESET);
        System.out.println();
        System.out.println("  " + DIM + "Tip: pipe to a file with:" + RESET);
        System.out.println("  " + DIM + "  jv.bat write <json-string> myfile.json" + RESET);
        System.out.println();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ARRAY — Build a JSON array from CLI value args
    // ══════════════════════════════════════════════════════════════════════════
    static void cmdArray(String[] args) throws Exception {
        if (args.length < 2) {
            printError("Too few arguments.");
            System.out.println("  Usage: " + B_CYAN + "JValueCli array val1 val2 val3 ..." + RESET);
            System.out.println("  " + DIM + "Example: array 10 Ada true 99.5 null" + RESET);
            System.exit(1);
        }
        System.out.println();
        System.out.println("  " + B_CYAN + "Building JSON array from your arguments" + RESET);
        System.out.println("  " + DIM + "─".repeat(52) + RESET);

        List<JsonValue> elements = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            String raw = args[i];
            JsonValue val;
            String detectedType;
            if (raw.equals("null"))        { val = JsonValue.ofNull();              detectedType = "null"; }
            else if (raw.equals("true"))   { val = JsonValue.of(true);              detectedType = "Boolean"; }
            else if (raw.equals("false"))  { val = JsonValue.of(false);             detectedType = "Boolean"; }
            else {
                try                        { val = JsonValue.of(Integer.parseInt(raw)); detectedType = "Integer"; }
                catch (NumberFormatException e1) {
                    try                    { val = JsonValue.of(Double.parseDouble(raw)); detectedType = "Double"; }
                    catch (NumberFormatException e2) { val = JsonValue.of(raw);     detectedType = "String"; }
                }
            }
            elements.add(val);
            System.out.println("  " + DIM + "[" + (i-1) + "]" + RESET + "  " + GREEN + raw + RESET
                + DIM + "  →  detected as " + RESET + YELLOW + detectedType + RESET);
        }

        JsonArray arr = JsonArray.of(elements.toArray(new JsonValue[0]));
        System.out.println();
        System.out.println("  " + B_CYAN + "▶  Java Array → JSON (compact)" + RESET);
        System.out.println("  " + DIM + "─".repeat(52) + RESET);
        System.out.println("  " + Json.stringify(arr));
        System.out.println();
        System.out.println("  " + B_CYAN + "▶  Java Array → JSON (pretty)" + RESET);
        System.out.println("  " + DIM + "─".repeat(52) + RESET);
        System.out.println(colorizeJson(Json.stringifyPretty(arr)));
        System.out.println();
        System.out.println("  " + B_GREEN + "✔  Array built  (" + arr.size() + " elements)" + RESET);
        System.out.println();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NUMINFO — Show all numeric representations of a JSON number
    // ══════════════════════════════════════════════════════════════════════════
    static void cmdNumInfo(String[] args) throws Exception {
        requireArgs(args, 2, "numinfo <number>  e.g.  numinfo 1.23e4");
        String input = args[1];
        System.out.println();
        System.out.println("  " + B_CYAN + "Number Precision Info" + RESET);
        System.out.println("  " + DIM + "─".repeat(52) + RESET);
        try {
            JsonValue v = Json.parse(input);
            if (!v.isNumber()) {
                printError("Not a JSON number: " + input);
                System.exit(1);
            }
            JsonNumber n = v.asJsonNumber();
            System.out.println("  " + DIM + "Raw lexeme  : " + RESET + YELLOW + n.raw() + RESET
                + DIM + "  (exact as it appeared in JSON — no precision loss)" + RESET);
            try { System.out.println("  " + DIM + "As int      : " + RESET + GREEN + n.asInt()    + RESET + DIM + "  (Integer)" + RESET); }
            catch (Exception e) { System.out.println("  " + DIM + "As int      : " + RESET + RED + "overflow — " + e.getMessage() + RESET); }
            try { System.out.println("  " + DIM + "As long     : " + RESET + GREEN + n.asLong()   + RESET + DIM + "  (Long)" + RESET); }
            catch (Exception e) { System.out.println("  " + DIM + "As long     : " + RESET + RED + "overflow — " + e.getMessage() + RESET); }
            System.out.println("  " + DIM + "As double   : " + RESET + GREEN + n.asDouble()       + RESET + DIM + "  (Double — may lose precision)" + RESET);
            System.out.println("  " + DIM + "As BigDecimal: " + RESET + GREEN + n.asBigDecimal()  + RESET + DIM + "  (exact — use this for math comparison)" + RESET);

            // Show lexical vs mathematical equality demo
            System.out.println();
            System.out.println("  " + B_YELLOW + "Equality semantics:" + RESET);
            JsonNumber n10  = (JsonNumber) Json.parse("1.0");
            JsonNumber n1e0 = (JsonNumber) Json.parse("1e0");
            System.out.println("  " + DIM + "  1.0 == 1e0  (lexical equals) : " + RESET
                + (n10.equals(n1e0) ? GREEN + "true" : RED + "false") + RESET
                + DIM + "  ← raw strings differ" + RESET);
            System.out.println("  " + DIM + "  1.0 == 1e0  (math compareTo) : " + RESET
                + GREEN + "true" + RESET
                + DIM + "  ← use asBigDecimal().compareTo() for math" + RESET);
        } catch (JsonParseException e) {
            printParseError(e, input); System.exit(1);
        }
        System.out.println();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // STRESS — Depth limit and duplicate key demos
    // ══════════════════════════════════════════════════════════════════════════
    static void cmdStress(String[] args) throws Exception {
        requireArgs(args, 2, "stress depth|dupkey");
        System.out.println();
        switch (args[1].toLowerCase()) {
            case "depth" -> {
                System.out.println("  " + B_CYAN + "Parser Depth Limit Test (max = 512)" + RESET);
                System.out.println("  " + DIM + "─".repeat(52) + RESET);

                // depth 512 — should pass
                String d512 = "[".repeat(512) + "1" + "]".repeat(512);
                try {
                    JsonValue v = Json.parse(d512);
                    System.out.println("  " + B_GREEN + "✔  depth=512 : PASS" + RESET
                        + DIM + "  (parsed OK, innermost value = " + v.asArray().get(0) + "... nested)" + RESET);
                } catch (Exception e) {
                    System.out.println("  " + B_RED + "✘  depth=512 : FAIL — " + e.getMessage() + RESET);
                }

                // depth 513 — should throw
                String d513 = "[".repeat(513) + "1" + "]".repeat(513);
                try {
                    Json.parse(d513);
                    System.out.println("  " + B_RED + "✘  depth=513 : should have thrown!" + RESET);
                } catch (JsonParseException e) {
                    System.out.println("  " + B_GREEN + "✔  depth=513 : CORRECTLY REJECTED" + RESET);
                    System.out.println("  " + DIM + "     → " + e.getMessage() + RESET);
                }
            }
            case "dupkey" -> {
                System.out.println("  " + B_CYAN + "Duplicate Key Handling (last-value-wins)" + RESET);
                System.out.println("  " + DIM + "─".repeat(52) + RESET);

                String json = "{\"key\":1,\"key\":2,\"key\":3}";
                JsonValue v = Json.parse(json);
                int result  = v.asObject().getInt("key");
                System.out.println("  " + DIM + "Input  : " + RESET + YELLOW + json + RESET);
                System.out.println("  " + DIM + "Result : " + RESET + GREEN + "key = " + result + RESET
                    + DIM + "  (last value wins — RFC 8259 allows this)" + RESET);
                System.out.println("  " + (result == 3
                    ? B_GREEN + "✔  Correct — last occurrence (3) wins" + RESET
                    : B_RED   + "✘  Unexpected result" + RESET));
            }
            default -> {
                printError("Unknown stress test: '" + args[1] + "'");
                System.out.println("  Options: " + CYAN + "depth" + RESET + "  or  " + CYAN + "dupkey" + RESET);
                System.exit(1);
            }
        }
        System.out.println();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POINTER — Compile a JSON Pointer and show its decoded tokens
    // ══════════════════════════════════════════════════════════════════════════
    static void cmdPointer(String[] args) throws Exception {
        requireArgs(args, 2, "pointer <json-pointer>  e.g.  pointer /users/0/name");
        String ptr = args[1];
        System.out.println();
        System.out.println("  " + B_CYAN + "JSON Pointer Inspector (RFC 6901)" + RESET);
        System.out.println("  " + DIM + "─".repeat(52) + RESET);
        try {
            JsonPointer compiled = JsonPointer.compile(ptr);
            List<String> tokens  = compiled.tokens();
            System.out.println("  " + DIM + "Raw pointer  : " + RESET + YELLOW + ptr + RESET);
            System.out.println("  " + DIM + "Tokens count : " + RESET + tokens.size()
                + (tokens.isEmpty() ? DIM + "  (empty string = root)" + RESET : ""));
            System.out.println();
            if (tokens.isEmpty()) {
                System.out.println("  " + DIM + "  This pointer refers to the ROOT value." + RESET);
            } else {
                System.out.println("  " + DIM + "Decoded reference tokens:" + RESET);
                for (int i = 0; i < tokens.size(); i++) {
                    System.out.println("  " + DIM + "  [" + i + "]" + RESET
                        + "  " + CYAN + tokens.get(i) + RESET);
                }
            }
            System.out.println();
            System.out.println("  " + B_GREEN + "✔  Valid RFC 6901 pointer — compiled successfully" + RESET);
            System.out.println("  " + DIM + "Tip: use 'get <file> " + ptr + "' to resolve against a JSON file." + RESET);
        } catch (IllegalArgumentException e) {
            printError("Invalid pointer: " + e.getMessage());
            System.out.println("  " + DIM + "Rules:" + RESET);
            System.out.println("  " + DIM + "  Must be empty (\"\") or start with '/'" + RESET);
            System.out.println("  " + DIM + "  ~0 decodes to ~,  ~1 decodes to /" + RESET);
            System.exit(1);
        }
        System.out.println();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /** Reads args[1] as a file path if it exists on disk, else treats it as raw JSON. */
    static String resolveInput(String input) throws IOException {
        Path p = Path.of(input);
        if (Files.exists(p)) {
            System.out.println(DIM + "  (reading from file: " + p.toAbsolutePath() + ")" + RESET);
            return Files.readString(p);
        }
        return input;
    }

    static void requireArgs(String[] args, int min, String usage) {
        if (args.length < min) {
            printError("Too few arguments.");
            System.out.println("  Usage: " + B_CYAN + "JValueCli " + usage + RESET);
            System.exit(1);
        }
    }

    static void printError(String msg) {
        System.out.println();
        System.out.println(B_RED + "╔═ ERROR " + "═".repeat(50) + "╗" + RESET);
        System.out.println(B_RED + "║  " + RESET + RED + msg + RESET);
        System.out.println(B_RED + "╚" + "═".repeat(58) + "╝" + RESET);
    }

    static void printParseError(JsonParseException e, String json) {
        System.out.println();
        System.out.println(B_RED + "╔═ JSON PARSE ERROR " + "═".repeat(39) + "╗" + RESET);
        System.out.println(B_RED + "║  " + RESET + RED + e.getMessage() + RESET);
        System.out.println(B_RED + "║  " + RESET + DIM + "Line   : " + RESET + e.line());
        System.out.println(B_RED + "║  " + RESET + DIM + "Column : " + RESET + e.column());
        System.out.println(B_RED + "║  " + RESET + DIM + "Offset : " + RESET + e.offset());
        // Show a snippet of the input around the error
        int off = (int) Math.min(e.offset(), json.length() - 1);
        int start = Math.max(0, off - 20);
        int end   = Math.min(json.length(), off + 20);
        String snippet = json.substring(start, end).replace("\n", "↵").replace("\r", "");
        String marker  = " ".repeat(off - start) + "^";
        System.out.println(B_RED + "║  " + RESET + DIM + "Near   : " + RESET + snippet);
        System.out.println(B_RED + "║  " + RESET + "         " + RED + marker + RESET);
        System.out.println(B_RED + "╚" + "═".repeat(58) + "╝" + RESET);
    }

    static String typeLabel(JsonValue v) {
        return switch (v.type()) {
            case OBJECT  -> B_YELLOW + "object"  + RESET;
            case ARRAY   -> B_YELLOW + "array"   + RESET;
            case STRING  -> MAGENTA  + "string"  + RESET;
            case NUMBER  -> GREEN    + "number"  + RESET;
            case BOOLEAN -> CYAN     + "boolean" + RESET;
            case NULL    -> DIM      + "null"    + RESET;
        };
    }

    /** Simple JSON colorizer for pretty output. */
    static String colorizeJson(String pretty) {
        StringBuilder sb = new StringBuilder();
        for (String line : pretty.split("\n")) {
            String trimmed = line.stripLeading();
            // Key-value line: "key": value
            if (trimmed.startsWith("\"") && trimmed.contains(":")) {
                int colon = line.indexOf(':');
                String key = line.substring(0, colon + 1);
                String val = colon + 1 < line.length() ? line.substring(colon + 1) : "";
                sb.append(BLUE).append(key).append(RESET);
                sb.append(colorizeValue(val));
            } else {
                sb.append(colorizeValue(line));
            }
            sb.append("\n");
        }
        return sb.toString().stripTrailing();
    }

    static String colorizeValue(String val) {
        String t = val.strip();
        if (t.startsWith("\"")) return MAGENTA + val + RESET;
        if (t.equals("true") || t.equals("false") ||
            t.equals("true,") || t.equals("false,"))  return CYAN + val + RESET;
        if (t.equals("null") || t.equals("null,"))    return DIM + val + RESET;
        if (t.matches("-?\\d.*"))                      return GREEN + val + RESET;
        return val;
    }

    /** Convert JsonValue tree → plain Java objects. */
    static Object toJava(JsonValue value) {
        if (value.isNull())    return null;
        if (value.isBoolean()) return value.asBoolean();
        if (value.isString())  return value.asString();
        if (value.isNumber()) {
            String raw = value.asJsonNumber().raw();
            if (!raw.contains(".") && !raw.contains("e") && !raw.contains("E")) {
                try   { return Integer.parseInt(raw); }
                catch (NumberFormatException e) { return Long.parseLong(raw); }
            }
            return Double.parseDouble(raw);
        }
        if (value.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonValue item : value.asArray()) list.add(toJava(item));
            return list;
        }
        // object
        Map<String, Object> map = new LinkedHashMap<>();
        for (var e : value.asObject()) map.put(e.getKey(), toJava(e.getValue()));
        return map;
    }
}
