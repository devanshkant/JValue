package com.jvalue.test;

/**
 * Test runner for JValue.
 *
 * <p>Since the JDK has no built-in test framework, this class serves as the
 * test harness. Each test class registers its tests here and the runner
 * executes them, reporting results.</p>
 *
 * <p>Usage: {@code java -cp build/classes;build/test-classes com.jvalue.test.TestRunner}</p>
 */
public final class TestRunner {

    private static int passed = 0;
    private static int failed = 0;
    private static int errors = 0;

    public static void main(String[] args) {
        System.out.println("=== JValue Test Suite ===");
        System.out.println();

        // Test classes will be registered here as they are created
        runTest("Smoke test", TestRunner::smokeTest);
        JsonValueTest.runAll();

        System.out.println();
        System.out.println("=== Results ===");
        System.out.printf("  Passed: %d%n", passed);
        System.out.printf("  Failed: %d%n", failed);
        System.out.printf("  Errors: %d%n", errors);
        System.out.printf("  Total:  %d%n", passed + failed + errors);

        if (failed > 0 || errors > 0) {
            System.out.println();
            System.out.println("[FAIL] Some tests did not pass.");
            System.exit(1);
        }

        System.out.println();
        System.out.println("[OK] All tests passed.");
    }

    /**
     * Runs a single named test.
     */
    public static void runTest(String name, Runnable test) {
        try {
            test.run();
            passed++;
            System.out.printf("  PASS  %s%n", name);
        } catch (AssertionError e) {
            failed++;
            System.out.printf("  FAIL  %s: %s%n", name, e.getMessage());
        } catch (Exception e) {
            errors++;
            System.out.printf("  ERROR %s: %s%n", name, e.toString());
        }
    }

    /**
     * Runs a batch of named tests from a test class.
     */
    public static void runSuite(String suiteName, Runnable suite) {
        System.out.printf("--- %s ---%n", suiteName);
        suite.run();
        System.out.println();
    }

    // --- Assertion utilities ---

    public static void assertEquals(Object expected, Object actual) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError("expected: <" + expected + "> but was: <" + actual + ">");
    }

    public static void assertEquals(String message, Object expected, Object actual) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError(message + " — expected: <" + expected + "> but was: <" + actual + ">");
    }

    public static void assertTrue(boolean condition) {
        if (!condition) throw new AssertionError("expected true but was false");
    }

    public static void assertTrue(String message, boolean condition) {
        if (!condition) throw new AssertionError(message);
    }

    public static void assertFalse(boolean condition) {
        if (condition) throw new AssertionError("expected false but was true");
    }

    public static void assertFalse(String message, boolean condition) {
        if (condition) throw new AssertionError(message);
    }

    public static void assertNull(Object obj) {
        if (obj != null) throw new AssertionError("expected null but was: <" + obj + ">");
    }

    public static void assertNotNull(Object obj) {
        if (obj == null) throw new AssertionError("expected non-null but was null");
    }

    public static void assertThrows(Class<? extends Throwable> expectedType, Runnable action) {
        try {
            action.run();
            throw new AssertionError("expected " + expectedType.getSimpleName() + " but nothing was thrown");
        } catch (Throwable t) {
            if (!expectedType.isInstance(t)) {
                throw new AssertionError(
                    "expected " + expectedType.getSimpleName() + " but got " + t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }
    }

    // --- Smoke test ---

    private static void smokeTest() {
        assertEquals("JValue 0.1.0", com.jvalue.Json.version());
    }
}
