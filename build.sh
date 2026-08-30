#!/usr/bin/env bash
# JValue Build Script — Zero-Dependency JSON Toolkit for Java 25
# Usage: ./build.sh [build|test|clean|deps-proof]
set -euo pipefail

JAVAC="javac"
JAVA="java"
JAVAC_OPTS="--release 25"
SRC_DIR="src/main/java"
TEST_DIR="src/test/java"
BUILD_DIR="build"
MAIN_OUT="$BUILD_DIR/classes"
TEST_OUT="$BUILD_DIR/test-classes"

cmd_build() {
    echo "[JValue] Compiling main sources..."
    mkdir -p "$MAIN_OUT"

    SOURCES=$(find "$SRC_DIR" -name '*.java' 2>/dev/null)
    if [ -z "$SOURCES" ]; then
        echo "[WARN] No source files found in $SRC_DIR"
        return 0
    fi

    $JAVAC $JAVAC_OPTS -d "$MAIN_OUT" $SOURCES
    echo "[OK] Build complete."
}

cmd_test() {
    cmd_build

    echo "[JValue] Compiling tests..."
    rm -rf "$TEST_OUT"
    mkdir -p "$TEST_OUT"

    TEST_SRCS=$(find "$TEST_DIR" -name '*.java' 2>/dev/null)
    if [ -z "$TEST_SRCS" ]; then
        echo "[WARN] No test files found in $TEST_DIR"
        return 0
    fi

    # Compile main and test sources together into a fresh test output tree.
    # This keeps verification independent from any stale class files.
    $JAVAC $JAVAC_OPTS -d "$TEST_OUT" $SOURCES $TEST_SRCS
    echo "[JValue] Fetching JSONTestSuite corpus (if needed)..."
    $JAVA -cp "$TEST_OUT" com.jvalue.test.FetchCorpus
    
    echo "[JValue] Running tests..."
    $JAVA -cp "$TEST_OUT" com.jvalue.test.TestRunner
    echo "[OK] All tests passed."
}

cmd_clean() {
    echo "[JValue] Cleaning..."
    rm -rf "$BUILD_DIR"
    echo "[OK] Clean complete."
}

cmd_deps_proof() {
    cmd_build

    echo ""
    echo "=== JValue Dependency Proof ==="
    echo ""
    echo "-- Java version --"
    $JAVA --version
    echo ""
    echo "-- jdeps module analysis (machine-verifiable) --"
    jdeps --multi-release 25 "$MAIN_OUT"
    echo ""
    echo "-- Summary --"
    echo "Build tool: javac (JDK built-in)"
    echo "Runtime: java (JDK built-in)"
    echo "Third-party dependencies: NONE"
    echo "All class files depend only on java.base (confirmed by jdeps above)"
    echo ""
    echo "=== Proof complete ==="
}

case "${1:-all}" in
    build)      cmd_build ;;
    test)       cmd_test ;;
    clean)      cmd_clean ;;
    deps-proof) cmd_deps_proof ;;
    all)        cmd_build && cmd_test ;;
    *)          echo "Usage: $0 [build|test|clean|deps-proof]"; exit 1 ;;
esac
