package com.jvalue;

/**
 * Thrown when the JSON parser encounters invalid input.
 *
 * <p>This exception provides the exact position of the error in the input:
 * {@link #line()}, {@link #column()}, and {@link #offset()}. All position
 * information is computed from the first character of input.</p>
 *
 * <p>This is an unchecked exception. JSON parse failures are typically data
 * errors that callers cannot meaningfully recover from inline, matching the
 * convention used by Jackson, Gson, and Go's {@code encoding/json}.</p>
 */
public class JsonParseException extends RuntimeException {

    private final int line;
    private final int column;
    private final long offset;

    /**
     * Creates a parse exception with position information.
     *
     * @param message human-readable description of the error
     * @param line    1-based line number where the error occurred
     * @param column  1-based column number where the error occurred
     * @param offset  0-based character offset from the start of input
     */
    public JsonParseException(String message, int line, int column, long offset) {
        super(formatMessage(message, line, column, offset));
        this.line = line;
        this.column = column;
        this.offset = offset;
    }

    /**
     * Returns the 1-based line number where the error occurred.
     */
    public int line() {
        return line;
    }

    /**
     * Returns the 1-based column number where the error occurred.
     */
    public int column() {
        return column;
    }

    /**
     * Returns the 0-based character offset from the start of input.
     */
    public long offset() {
        return offset;
    }

    private static String formatMessage(String message, int line, int column, long offset) {
        return message + " at line " + line + ", column " + column + " (offset " + offset + ")";
    }
}
