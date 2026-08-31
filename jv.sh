#!/usr/bin/env bash
# JValue CLI launcher — auto-compiles if cli/classes is missing
# Usage: ./jv.sh <command> [args]

set -e

# Locate project root relative to this script
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || "$OSTYPE" == "win32" ]]; then
    CP="build/classes;cli/classes"
else
    CP="build/classes:cli/classes"
fi

# Auto-build JValue library if not yet compiled
if [ ! -f "build/classes/com/jvalue/Json.class" ]; then
    echo "[jv] Building JValue library..."
    ./build.sh build
fi

# Auto-compile CLI if not yet compiled
if [ ! -f "cli/classes/JValueCli.class" ]; then
    echo "[jv] Compiling CLI..."
    mkdir -p cli/classes
    if ! javac --release 25 -cp "build/classes" -d "cli/classes" "cli/JValueCli.java"; then
        echo "[jv] CLI compile FAILED."
        exit 1
    fi
    echo "[jv] CLI ready."
fi

# Run the CLI, forwarding all arguments, and propagate the exit code by using exec
exec java -cp "$CP" JValueCli "$@"
