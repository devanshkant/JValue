package com.jvalue;

/**
 * A character-by-character input source with position tracking.
 *
 * <p>Wraps a {@link String} and provides peek/advance operations while
 * maintaining line, column, and offset counters for error reporting.
 * Position tracking is wired in from the first character, per Track B guidance.</p>
 *
 * <p>This class is package-private. Only the parser uses it.</p>
 */
final class CharSource {

    private final String input;
    private int pos;
    private int line;
    private int column;

    /**
     * Creates a CharSource over the given input string.
     *
     * @param input the JSON text to parse (must not be null)
     */
    CharSource(String input) {
        this.input = java.util.Objects.requireNonNull(input, "Input must not be null");
        this.pos = 0;
        this.line = 1;
        this.column = 1;
    }

    /**
     * Returns the current character without consuming it.
     *
     * @throws JsonParseException if the input is exhausted
     */
    char peek() {
        if (isAtEnd()) {
            throw error("Unexpected end of input");
        }
        return input.charAt(pos);
    }

    /**
     * Returns the current character if available, or {@code -1} if at end.
     * Does not consume the character.
     */
    int peekOrEnd() {
        return isAtEnd() ? -1 : input.charAt(pos);
    }

    /**
     * Consumes and returns the current character, updating position counters.
     *
     * @throws JsonParseException if the input is exhausted
     */
    char advance() {
        if (isAtEnd()) {
            throw error("Unexpected end of input");
        }
        char c = input.charAt(pos);
        pos++;
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    /**
     * Consumes the current character, asserting it matches the expected character.
     *
     * @param expected the character that must be at the current position
     * @throws JsonParseException if the input is exhausted or the character does not match
     */
    void expect(char expected) {
        if (isAtEnd()) {
            throw error("Expected '" + expected + "' but reached end of input");
        }
        char c = input.charAt(pos);
        if (c != expected) {
            throw error("Expected '" + expected + "' but found '" + c + "'");
        }
        advance();
    }

    /**
     * Skips JSON whitespace: space (0x20), tab (0x09), newline (0x0A),
     * carriage return (0x0D).
     */
    void skipWhitespace() {
        while (!isAtEnd()) {
            char c = input.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                advance();
            } else {
                break;
            }
        }
    }

    /**
     * Returns true if all input has been consumed.
     */
    boolean isAtEnd() {
        return pos >= input.length();
    }

    /**
     * Returns the 1-based line number of the current position.
     */
    int line() {
        return line;
    }

    /**
     * Returns the 1-based column number of the current position.
     */
    int column() {
        return column;
    }

    /**
     * Returns the 0-based character offset of the current position.
     */
    long offset() {
        return pos;
    }

    /**
     * Returns a substring of the original input between the given offsets.
     * Used by the number parser to extract the raw numeric lexeme.
     *
     * @param start inclusive start offset (0-based)
     * @param end   exclusive end offset (0-based)
     */
    String substring(int start, int end) {
        return input.substring(start, end);
    }

    /**
     * Creates a {@link JsonParseException} at the current position.
     */
    JsonParseException error(String message) {
        return new JsonParseException(message, line, column, pos);
    }
}
