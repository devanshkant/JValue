package com.jvalue.test;

import com.jvalue.Json;
import com.jvalue.JsonParseException;

import java.io.IOException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import static com.jvalue.test.TestRunner.*;

/**
 * Conformance test runner for the JSONTestSuite corpus.
 *
 * <p>Discovers test files from {@code test-data/JSONTestSuite/test_parsing/},
 * classifies expected outcomes by filename prefix, and runs each file
 * through {@link Json#parse(String)}.</p>
 *
 * <p>If the corpus directory is absent (e.g., offline builds without the
 * download step), the entire suite is gracefully skipped.</p>
 */
public final class JsonConformanceTest {

    private static final Path CORPUS_DIR = Path.of("test-data", "JSONTestSuite", "test_parsing");

    /**
     * i_ files where JValue policy is REJECT (lone/invalid surrogates).
     */
    private static final Set<String> I_REJECT = Set.of(
        "i_string_1st_surrogate_but_2nd_missing.json",
        "i_string_1st_valid_surrogate_2nd_invalid.json",
        "i_string_incomplete_surrogate_and_escape_valid.json",
        "i_string_incomplete_surrogates_escape_valid.json",
        "i_string_incomplete_surrogate_pair.json",
        "i_string_invalid_lonely_surrogate.json",
        "i_string_invalid_surrogate.json",
        "i_string_inverted_surrogates_U+1D11E.json",
        "i_string_lone_second_surrogate.json",
        "i_object_key_lone_2nd_surrogate.json"
    );

    /**
     * i_ files where JValue policy is ACCEPT (valid syntax, implementation-defined semantics).
     */
    private static final Set<String> I_ACCEPT = Set.of(
        "i_number_double_huge_neg_exp.json",
        "i_number_huge_exp.json",
        "i_number_neg_int_huge_exp.json",
        "i_number_pos_double_huge_exp.json",
        "i_number_real_neg_overflow.json",
        "i_number_real_pos_overflow.json",
        "i_number_real_underflow.json",
        "i_number_too_big_neg_int.json",
        "i_number_too_big_pos_int.json",
        "i_number_very_big_negative_int.json",
        "i_structure_500_nested_arrays.json"
    );

    /**
     * i_ files where JValue policy is REJECT (BOM, not valid JSON start).
     */
    private static final Set<String> I_REJECT_OTHER = Set.of(
        "i_structure_UTF-8_BOM_empty_object.json"
    );

    /**
     * i_ files that are not applicable to String-based parsing (raw UTF-8 byte issues).
     * These are skipped with an explanation.
     */
    private static final Set<String> I_SKIP = Set.of(
        "i_string_invalid_utf-8.json",
        "i_string_iso_latin_1.json",
        "i_string_lone_utf8_continuation_byte.json",
        "i_string_not_in_unicode_range.json",
        "i_string_overlong_sequence_2_bytes.json",
        "i_string_overlong_sequence_6_bytes.json",
        "i_string_overlong_sequence_6_bytes_null.json",
        "i_string_truncated-utf-8.json",
        "i_string_UTF-16LE_with_BOM.json",
        "i_string_UTF-8_invalid_sequence.json",
        "i_string_utf16BE_no_BOM.json",
        "i_string_utf16LE_no_BOM.json",
        "i_string_UTF8_surrogate_U+D800.json"
    );

    private static int cPassed = 0;
    private static int cFailed = 0;
    private static int cSkipped = 0;

    public static void runAll() {
        runSuite("JSONTestSuite Conformance", () -> {
            if (!Files.exists(CORPUS_DIR)) {
                System.out.println("  SKIP  JSONTestSuite corpus not found at " + CORPUS_DIR);
                System.out.println("        Run: java -cp build/test-classes com.jvalue.test.FetchCorpus");
                System.out.println("        Or:  git clone https://github.com/nst/JSONTestSuite test-data/JSONTestSuite");
                return;
            }

            cPassed = 0;
            cFailed = 0;
            cSkipped = 0;

            try (Stream<Path> files = Files.list(CORPUS_DIR)) {
                files.filter(p -> p.toString().endsWith(".json"))
                     .sorted()
                     .forEach(JsonConformanceTest::runSingleFile);
            } catch (IOException e) {
                System.out.println("  ERROR Failed to list corpus directory: " + e.getMessage());
                return;
            }

            System.out.println();
            System.out.printf("  Conformance: %d passed, %d failed, %d skipped%n",
                    cPassed, cFailed, cSkipped);

            if (cFailed > 0) {
                throw new AssertionError(cFailed + " conformance test(s) failed");
            }
        });
    }

    private static void runSingleFile(Path file) {
        String name = file.getFileName().toString();

        if (name.startsWith("y_")) {
            runExpectAccept(file, name);
        } else if (name.startsWith("n_")) {
            runExpectReject(file, name);
        } else if (name.startsWith("i_")) {
            runImplementationDefined(file, name);
        }
    }

    /**
     * y_ files: MUST parse successfully.
     */
    private static void runExpectAccept(Path file, String name) {
        try {
            String content = readFileLenient(file);
            Json.parse(content);
            cPassed++;
        } catch (JsonParseException e) {
            cFailed++;
            System.out.printf("  FAIL  %s (should accept): %s%n", name, e.getMessage());
        } catch (IOException e) {
            cFailed++;
            System.out.printf("  FAIL  %s (IO error): %s%n", name, e.getMessage());
        }
    }

    /**
     * n_ files: MUST throw JsonParseException.
     */
    private static void runExpectReject(Path file, String name) {
        try {
            String content = readFileLenient(file);
            Json.parse(content);
            cFailed++;
            System.out.printf("  FAIL  %s (should reject but accepted)%n", name);
        } catch (JsonParseException e) {
            cPassed++;
        } catch (IOException e) {
            cFailed++;
            System.out.printf("  FAIL  %s (IO error): %s%n", name, e.getMessage());
        }
    }

    /**
     * i_ files: implementation-defined. Check against JValue's policy table.
     */
    private static void runImplementationDefined(Path file, String name) {
        if (I_SKIP.contains(name)) {
            cSkipped++;
            return;
        }

        boolean expectReject = I_REJECT.contains(name) || I_REJECT_OTHER.contains(name);
        boolean expectAccept = I_ACCEPT.contains(name);

        if (!expectReject && !expectAccept) {
            // Unknown i_ file — skip with warning
            cSkipped++;
            System.out.printf("  SKIP  %s (no policy defined)%n", name);
            return;
        }

        try {
            String content = readFileLenient(file);
            Json.parse(content);
            // Parsed successfully
            if (expectReject) {
                cFailed++;
                System.out.printf("  FAIL  %s [policy: REJECT] but accepted%n", name);
            } else {
                cPassed++;
            }
        } catch (JsonParseException e) {
            // Parse failed
            if (expectAccept) {
                cFailed++;
                System.out.printf("  FAIL  %s [policy: ACCEPT] but rejected: %s%n", name, e.getMessage());
            } else {
                cPassed++;
            }
        } catch (IOException e) {
            cFailed++;
            System.out.printf("  FAIL  %s (IO error): %s%n", name, e.getMessage());
        }
    }

    /**
     * Reads a file as UTF-8 with lenient decoding.
     *
     * <p>Some corpus files contain invalid UTF-8 byte sequences intentionally.
     * {@link Files#readString} uses a strict decoder that throws on malformed input.
     * This method uses {@link CodingErrorAction#REPLACE} to substitute replacement
     * characters (U+FFFD) for invalid bytes, allowing the file to be read as a
     * Java String. The resulting string will then fail JSON parsing on its own
     * merits (unexpected characters), which is the correct behavior.</p>
     */
    private static String readFileLenient(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        return decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString();
    }
}
