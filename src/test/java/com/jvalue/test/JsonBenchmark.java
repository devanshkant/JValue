package com.jvalue.test;

import com.jvalue.Json;
import com.jvalue.JsonPointer;
import com.jvalue.JsonValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * JDK-only performance benchmark for JValue — Phase 9.
 *
 * <p>
 * Uses {@link System#nanoTime()} with warmup and measurement iterations.
 * This is a test-scope class and is NOT part of the production artifact.
 * </p>
 *
 * <p>
 * Usage: {@code java -cp build/test-classes com.jvalue.test.JsonBenchmark}
 * </p>
 */
public final class JsonBenchmark {

    // --- Configuration ---
    private static final int WARMUP_ITERATIONS = 500;
    private static final int MEASUREMENT_ITERATIONS = 1000;
    private static final int WARMUP_ITERATIONS_LARGE = 50;
    private static final int MEASUREMENT_ITERATIONS_LARGE = 100;

    public static void main(String[] args) {
        System.out.println("=== JValue Performance Benchmark — Phase 9 ===");
        System.out.println();
        printEnvironment();
        System.out.println();

        // --- Generate benchmark inputs ---
        String smallJson = generateSmallJson();
        String mediumJson = generateMediumJson();
        String largeJson = generateLargeJson();
        String deepJson = generateDeepJson(400);
        String stringHeavyJson = generateStringHeavyJson();

        // Pre-parse trees for serializer benchmarks
        JsonValue smallTree = Json.parse(smallJson);
        JsonValue mediumTree = Json.parse(mediumJson);
        JsonValue largeTree = Json.parse(largeJson);
        JsonValue deepTree = Json.parse(deepJson);
        JsonValue stringHeavyTree = Json.parse(stringHeavyJson);

        // --- Parser benchmarks ---
        System.out.println("--- Parser Throughput ---");
        System.out.println();
        benchmarkParse("Small (~" + smallJson.length() + " B)", smallJson,
                WARMUP_ITERATIONS, MEASUREMENT_ITERATIONS);
        benchmarkParse("Medium (~" + formatSize(mediumJson.length()) + ")", mediumJson,
                WARMUP_ITERATIONS, MEASUREMENT_ITERATIONS);
        benchmarkParse("Large (~" + formatSize(largeJson.length()) + ")", largeJson,
                WARMUP_ITERATIONS_LARGE, MEASUREMENT_ITERATIONS_LARGE);
        benchmarkParse("Deep nesting (400 levels)", deepJson,
                WARMUP_ITERATIONS, MEASUREMENT_ITERATIONS);
        benchmarkParse("String-heavy (~" + formatSize(stringHeavyJson.length()) + ")", stringHeavyJson,
                WARMUP_ITERATIONS, MEASUREMENT_ITERATIONS);
        System.out.println();

        // --- Serializer benchmarks (compact) ---
        System.out.println("--- Serializer Throughput (Compact) ---");
        System.out.println();
        benchmarkSerialize("Small", smallTree, false,
                WARMUP_ITERATIONS, MEASUREMENT_ITERATIONS);
        benchmarkSerialize("Medium", mediumTree, false,
                WARMUP_ITERATIONS, MEASUREMENT_ITERATIONS);
        benchmarkSerialize("Large", largeTree, false,
                WARMUP_ITERATIONS_LARGE, MEASUREMENT_ITERATIONS_LARGE);
        benchmarkSerialize("Deep nesting", deepTree, false,
                WARMUP_ITERATIONS, MEASUREMENT_ITERATIONS);
        System.out.println();

        // --- Serializer benchmarks (pretty) ---
        System.out.println("--- Serializer Throughput (Pretty) ---");
        System.out.println();
        benchmarkSerialize("Small", smallTree, true,
                WARMUP_ITERATIONS, MEASUREMENT_ITERATIONS);
        benchmarkSerialize("Medium", mediumTree, true,
                WARMUP_ITERATIONS, MEASUREMENT_ITERATIONS);
        benchmarkSerialize("Large", largeTree, true,
                WARMUP_ITERATIONS_LARGE, MEASUREMENT_ITERATIONS_LARGE);
        benchmarkSerialize("Deep nesting", deepTree, true,
                WARMUP_ITERATIONS, MEASUREMENT_ITERATIONS);
        System.out.println();

        // --- JSON Pointer benchmarks ---
        System.out.println("--- JSON Pointer Lookup ---");
        System.out.println();
        benchmarkPointer("Shallow (depth 1)", mediumTree, "/users/0/name",
                WARMUP_ITERATIONS, MEASUREMENT_ITERATIONS);
        benchmarkPointer("Medium (depth 3)", mediumTree, "/users/4/address/city",
                WARMUP_ITERATIONS, MEASUREMENT_ITERATIONS);
        benchmarkPointer("Root (empty pointer)", mediumTree, "",
                WARMUP_ITERATIONS, MEASUREMENT_ITERATIONS);
        System.out.println();

        // --- Round-trip benchmark ---
        System.out.println("--- Round-Trip (Parse → Stringify → Parse) ---");
        System.out.println();
        benchmarkRoundTrip("Small", smallJson,
                WARMUP_ITERATIONS, MEASUREMENT_ITERATIONS);
        benchmarkRoundTrip("Medium", mediumJson,
                WARMUP_ITERATIONS, MEASUREMENT_ITERATIONS);
        benchmarkRoundTrip("Large", largeJson,
                WARMUP_ITERATIONS_LARGE, MEASUREMENT_ITERATIONS_LARGE);
        System.out.println();

        // --- Pretty vs Compact overhead ---
        System.out.println("--- Pretty vs Compact Overhead ---");
        System.out.println();
        comparePrettyOverhead("Medium", mediumTree,
                WARMUP_ITERATIONS, MEASUREMENT_ITERATIONS);
        comparePrettyOverhead("Large", largeTree,
                WARMUP_ITERATIONS_LARGE, MEASUREMENT_ITERATIONS_LARGE);
        System.out.println();

        // --- File API benchmarks ---
        System.out.println("--- File API ---");
        System.out.println();
        benchmarkFileApis(mediumJson, mediumTree);
        System.out.println();

        // --- Stress characteristics ---
        System.out.println("--- Stress Characteristics ---");
        System.out.println();
        benchmarkStress(largeTree, stringHeavyTree, deepTree);
        System.out.println();

        System.out.println("=== Benchmark Complete ===");
    }

    // --- Benchmark runners ---

    private static void benchmarkParse(String label, String input,
            int warmup, int measure) {
        // Warmup
        for (int i = 0; i < warmup; i++) {
            Json.parse(input);
        }

        // Measure
        long[] timings = new long[measure];
        for (int i = 0; i < measure; i++) {
            long start = System.nanoTime();
            Json.parse(input);
            timings[i] = System.nanoTime() - start;
        }

        printResults("Parse " + label, timings, input.length());
    }

    private static void benchmarkSerialize(String label, JsonValue tree, boolean pretty,
            int warmup, int measure) {
        // Warmup
        for (int i = 0; i < warmup; i++) {
            if (pretty) {
                Json.stringifyPretty(tree);
            } else {
                Json.stringify(tree);
            }
        }

        // Measure
        long[] timings = new long[measure];
        int outputSize = 0;
        for (int i = 0; i < measure; i++) {
            long start = System.nanoTime();
            String result;
            if (pretty) {
                result = Json.stringifyPretty(tree);
            } else {
                result = Json.stringify(tree);
            }
            timings[i] = System.nanoTime() - start;
            if (i == 0)
                outputSize = result.length();
        }

        printResults("Serialize " + label, timings, outputSize);
    }

    private static void benchmarkPointer(String label, JsonValue tree, String pointer,
            int warmup, int measure) {
        JsonPointer compiled = JsonPointer.compile(pointer);

        // Warmup
        for (int i = 0; i < warmup; i++) {
            compiled.query(tree);
        }

        // Measure
        long[] timings = new long[measure];
        for (int i = 0; i < measure; i++) {
            long start = System.nanoTime();
            compiled.query(tree);
            timings[i] = System.nanoTime() - start;
        }

        printTimingResults("Pointer " + label, timings);
    }

    private static void benchmarkRoundTrip(String label, String input,
            int warmup, int measure) {
        // Warmup
        for (int i = 0; i < warmup; i++) {
            Json.parse(Json.stringify(Json.parse(input)));
        }

        // Measure
        long[] timings = new long[measure];
        for (int i = 0; i < measure; i++) {
            long start = System.nanoTime();
            Json.parse(Json.stringify(Json.parse(input)));
            timings[i] = System.nanoTime() - start;
        }

        printResults("Round-trip " + label, timings, input.length());
    }

    private static void benchmarkFileApis(String mediumJson, JsonValue mediumTree) {
        try {
            Path tempFile = Files.createTempFile("jvalue-bench-", ".json");
            try {
                // Write the medium JSON so we have a file to read
                Json.writeFile(mediumTree, tempFile);

                // File read benchmark
                int warmup = 200;
                int measure = 500;

                for (int i = 0; i < warmup; i++)
                    Json.read(tempFile);
                long[] readTimings = new long[measure];
                for (int i = 0; i < measure; i++) {
                    long start = System.nanoTime();
                    Json.read(tempFile);
                    readTimings[i] = System.nanoTime() - start;
                }
                long fileSize = Files.size(tempFile);
                printResults("File read (~" + formatSize((int) fileSize) + ")",
                        readTimings, (int) fileSize);

                // File write compact benchmark
                for (int i = 0; i < warmup; i++)
                    Json.writeFile(mediumTree, tempFile);
                long[] writeTimings = new long[measure];
                for (int i = 0; i < measure; i++) {
                    long start = System.nanoTime();
                    Json.writeFile(mediumTree, tempFile);
                    writeTimings[i] = System.nanoTime() - start;
                }
                String compactOut = Json.stringify(mediumTree);
                printResults("File write compact (~" + formatSize(compactOut.length()) + ")",
                        writeTimings, compactOut.length());

                // File write pretty benchmark
                for (int i = 0; i < warmup; i++)
                    Json.writePrettyFile(mediumTree, tempFile);
                long[] prettyTimings = new long[measure];
                for (int i = 0; i < measure; i++) {
                    long start = System.nanoTime();
                    Json.writePrettyFile(mediumTree, tempFile);
                    prettyTimings[i] = System.nanoTime() - start;
                }
                String prettyOut = Json.stringifyPretty(mediumTree);
                printResults("File write pretty (~" + formatSize(prettyOut.length()) + ")",
                        prettyTimings, prettyOut.length());
            } finally {
                Files.deleteIfExists(tempFile);
            }
        } catch (IOException e) {
            System.out.println("  SKIP  File API benchmarks: " + e.getMessage());
        }
    }

    private static void benchmarkStress(JsonValue largeTree,
            JsonValue stringHeavyTree, JsonValue deepTree) {
        // Large collection serialization
        int warmup = 20;
        int measure = 50;

        for (int i = 0; i < warmup; i++)
            Json.stringify(largeTree);
        long[] largeTimings = new long[measure];
        int largeSize = 0;
        for (int i = 0; i < measure; i++) {
            long start = System.nanoTime();
            String result = Json.stringify(largeTree);
            largeTimings[i] = System.nanoTime() - start;
            if (i == 0)
                largeSize = result.length();
        }
        printResults("Large collection stringify", largeTimings, largeSize);

        // String-heavy serialization
        for (int i = 0; i < warmup; i++)
            Json.stringify(stringHeavyTree);
        long[] strTimings = new long[measure];
        int strSize = 0;
        for (int i = 0; i < measure; i++) {
            long start = System.nanoTime();
            String result = Json.stringify(stringHeavyTree);
            strTimings[i] = System.nanoTime() - start;
            if (i == 0)
                strSize = result.length();
        }
        printResults("String-heavy stringify", strTimings, strSize);

        // Deep nesting parse + serialize
        for (int i = 0; i < 200; i++) {
            Json.stringify(deepTree);
        }
        long[] deepTimings = new long[500];
        int deepSize = 0;
        for (int i = 0; i < 500; i++) {
            long start = System.nanoTime();
            String result = Json.stringify(deepTree);
            deepTimings[i] = System.nanoTime() - start;
            if (i == 0)
                deepSize = result.length();
        }
        printResults("Deep nesting (400) stringify", deepTimings, deepSize);
    }

    private static void comparePrettyOverhead(String label, JsonValue tree,
            int warmup, int measure) {
        // Compact
        for (int i = 0; i < warmup; i++)
            Json.stringify(tree);
        long[] compactTimings = new long[measure];
        for (int i = 0; i < measure; i++) {
            long start = System.nanoTime();
            Json.stringify(tree);
            compactTimings[i] = System.nanoTime() - start;
        }

        // Pretty
        for (int i = 0; i < warmup; i++)
            Json.stringifyPretty(tree);
        long[] prettyTimings = new long[measure];
        for (int i = 0; i < measure; i++) {
            long start = System.nanoTime();
            Json.stringifyPretty(tree);
            prettyTimings[i] = System.nanoTime() - start;
        }

        Arrays.sort(compactTimings);
        Arrays.sort(prettyTimings);
        long compactMedian = compactTimings[measure / 2];
        long prettyMedian = prettyTimings[measure / 2];
        double overhead = (prettyMedian - compactMedian) * 100.0 / compactMedian;

        System.out.printf("  %s: compact median=%s, pretty median=%s, overhead=%.1f%%%n",
                label, formatNs(compactMedian), formatNs(prettyMedian), overhead);
    }

    // --- Result reporting ---

    private static void printResults(String label, long[] timings, int dataSize) {
        Arrays.sort(timings);
        long min = timings[0];
        long max = timings[timings.length - 1];
        long median = timings[timings.length / 2];
        double mean = 0;
        for (long t : timings)
            mean += t;
        mean /= timings.length;

        double variance = 0;
        for (long t : timings) {
            double diff = t - mean;
            variance += diff * diff;
        }
        double stddev = Math.sqrt(variance / timings.length);

        double opsPerSec = 1_000_000_000.0 / mean;
        double mbPerSec = (dataSize / (1024.0 * 1024.0)) / (mean / 1_000_000_000.0);

        System.out.printf("  %-35s median=%s  mean=%s  stddev=%s  min=%s  max=%s%n",
                label + ":", formatNs(median), formatNs(mean), formatNs(stddev),
                formatNs(min), formatNs(max));
        System.out.printf("  %-35s %.0f ops/sec", "", opsPerSec);
        if (dataSize >= 1024) {
            System.out.printf("  %.1f MB/s", mbPerSec);
        }
        System.out.println();
    }

    private static void printTimingResults(String label, long[] timings) {
        Arrays.sort(timings);
        long min = timings[0];
        long max = timings[timings.length - 1];
        long median = timings[timings.length / 2];
        double mean = 0;
        for (long t : timings)
            mean += t;
        mean /= timings.length;

        double opsPerSec = 1_000_000_000.0 / mean;

        System.out.printf("  %-35s median=%s  mean=%s  min=%s  max=%s  (%.0f ops/sec)%n",
                label + ":", formatNs(median), formatNs(mean), formatNs(min),
                formatNs(max), opsPerSec);
    }

    // --- Input generators ---

    /**
     * Small: a typical JSON object (~100 bytes).
     */
    private static String generateSmallJson() {
        return """
                {"name":"JValue","version":1,"features":["parse","stringify","pretty"],"zeroDeps":true,"license":"MIT"}""";
    }

    /**
     * Medium: a realistic JSON document (~10 KB) — array of user objects.
     */
    private static String generateMediumJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"users\":[");
        for (int i = 0; i < 50; i++) {
            if (i > 0)
                sb.append(',');
            sb.append(String.format("""
                    {"id":%d,"name":"User %d","email":"user%d@example.com",\
                    "active":%s,"score":%d.%d,\
                    "tags":["java","json","zero-deps"],\
                    "address":{"street":"%d Main St","city":"Metropolis","zip":"%05d"}}""",
                    i, i, i, i % 2 == 0, i * 17, i * 3, i * 100, i * 111));
        }
        sb.append("]}");
        return sb.toString();
    }

    /**
     * Large: ~1 MB array of objects.
     */
    private static String generateLargeJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        int count = 0;
        while (sb.length() < 1_000_000) {
            if (count > 0)
                sb.append(',');
            sb.append(String.format("""
                    {"id":%d,"type":"record","value":%d.%d,\
                    "label":"Item number %d for benchmark testing",\
                    "nested":{"a":%d,"b":%d,"c":%d},\
                    "tags":["benchmark","large","test","data","performance"]}""",
                    count, count * 7, count * 3, count, count, count + 1, count + 2));
            count++;
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Deep nesting: arrays nested to the specified depth.
     */
    private static String generateDeepJson(int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++)
            sb.append('[');
        sb.append("42");
        for (int i = 0; i < depth; i++)
            sb.append(']');
        return sb.toString();
    }

    /**
     * String-heavy: JSON with many escape sequences and Unicode.
     */
    private static String generateStringHeavyJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < 200; i++) {
            if (i > 0)
                sb.append(',');
            sb.append('"');
            // Mix of plain text, escapes, and unicode escapes
            sb.append("Hello\\nWorld\\t");
            sb.append("line\\r\\n");
            sb.append("quote\\\"here\\\"");
            sb.append("backslash\\\\path");
            sb.append("tab\\there");
            sb.append("\\u0041\\u0042\\u0043"); // ABC as unicode escapes
            sb.append("mixed text with \\u00e9\\u00e8\\u00ea"); // accented chars
            sb.append('"');
        }
        sb.append("]");
        return sb.toString();
    }

    // --- Formatting ---

    private static String formatNs(long ns) {
        if (ns < 1_000)
            return ns + " ns";
        if (ns < 1_000_000)
            return String.format("%.1f µs", ns / 1_000.0);
        if (ns < 1_000_000_000)
            return String.format("%.2f ms", ns / 1_000_000.0);
        return String.format("%.2f s", ns / 1_000_000_000.0);
    }

    private static String formatNs(double ns) {
        return formatNs((long) ns);
    }

    private static String formatSize(int bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static void printEnvironment() {
        System.out.println("Benchmark Environment:");
        System.out.printf("  OS:      %s %s (%s)%n",
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"));
        System.out.printf("  JDK:     %s %s%n",
                System.getProperty("java.vm.name"),
                System.getProperty("java.runtime.version"));
        System.out.printf("  VM:      %s%n",
                System.getProperty("java.vm.info"));
        System.out.printf("  Procs:   %d%n",
                Runtime.getRuntime().availableProcessors());
        System.out.printf("  MaxMem:  %d MB%n",
                Runtime.getRuntime().maxMemory() / (1024 * 1024));
        System.out.printf("  Warmup:  %d / %d iterations (small/large)%n",
                WARMUP_ITERATIONS, WARMUP_ITERATIONS_LARGE);
        System.out.printf("  Measure: %d / %d iterations (small/large)%n",
                MEASUREMENT_ITERATIONS, MEASUREMENT_ITERATIONS_LARGE);
    }
}
