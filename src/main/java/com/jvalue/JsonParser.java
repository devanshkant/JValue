package com.jvalue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A recursive-descent JSON parser following RFC 8259.
 *
 * <p>Each JSON production rule maps to a {@code parse*()} method.
 * The parser operates on a {@link CharSource} that tracks position
 * for error reporting. All parsed values are returned as instances
 * of the {@link JsonValue} sealed hierarchy.</p>
 *
 * <p>This class is package-private. Use {@link Json#parse(String)} instead.</p>
 */
final class JsonParser {

    /**
     * Maximum allowed nesting depth for arrays and objects.
     * Prevents stack overflow from adversarial input.
     */
    private static final int MAX_DEPTH = 512;

    private final CharSource source;
    private int depth;

    JsonParser(CharSource source) {
        this.source = source;
        this.depth = 0;
    }

    /**
     * Parses a complete JSON document: one value followed by optional whitespace.
     * Rejects trailing data after the root value.
     */
    JsonValue parseDocument() {
        source.skipWhitespace();
        if (source.isAtEnd()) {
            throw source.error("Empty input");
        }
        JsonValue value = parseValue();
        source.skipWhitespace();
        if (!source.isAtEnd()) {
            throw source.error("Unexpected data after JSON value");
        }
        return value;
    }

    /**
     * Parses a single JSON value by dispatching on the first character.
     */
    private JsonValue parseValue() {
        char c = source.peek();
        return switch (c) {
            case '"' -> parseString();
            case '{' -> parseObject();
            case '[' -> parseArray();
            case 't', 'f' -> parseBoolean();
            case 'n' -> parseNull();
            case '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> parseNumber();
            default -> throw source.error("Unexpected character '" + c + "'");
        };
    }

    // --- Primitive parsers ---

    /**
     * Parses the literal {@code null}.
     */
    private JsonNull parseNull() {
        expectLiteral("null");
        return JsonNull.INSTANCE;
    }

    /**
     * Parses {@code true} or {@code false}.
     */
    private JsonBoolean parseBoolean() {
        if (source.peek() == 't') {
            expectLiteral("true");
            return JsonBoolean.TRUE;
        } else {
            expectLiteral("false");
            return JsonBoolean.FALSE;
        }
    }

    /**
     * Consumes the exact literal string, or throws with position info.
     */
    private void expectLiteral(String literal) {
        for (int i = 0; i < literal.length(); i++) {
            if (source.isAtEnd()) {
                throw source.error("Expected '" + literal + "' but reached end of input");
            }
            char c = source.advance();
            if (c != literal.charAt(i)) {
                throw source.error("Expected '" + literal + "' but found unexpected character '" + c + "'");
            }
        }
    }

    // --- String parser ---

    /**
     * Parses a JSON string: {@code "..."} with escape sequence handling.
     *
     * <p>Handles all RFC 8259 escape sequences:</p>
     * <ul>
     *   <li>Single-character escapes: {@code \" \\ \/ \b \f \n \r \t}</li>
     *   <li>Unicode escapes: backslash-u followed by 4 hex digits</li>
     *   <li>Surrogate pairs: high surrogate (U+D800-U+DBFF) followed by low surrogate (U+DC00-U+DFFF)</li>
     * </ul>
     *
     * <p>Rejects unescaped control characters U+0000–U+001F per RFC 8259.</p>
     */
    private JsonString parseString() {
        source.expect('"');
        StringBuilder sb = new StringBuilder();

        while (true) {
            if (source.isAtEnd()) {
                throw source.error("Unterminated string");
            }
            char c = source.advance();

            if (c == '"') {
                return new JsonString(sb.toString());
            }

            if (c == '\\') {
                sb.append(parseEscapeSequence());
            } else if (c <= '\u001F') {
                // RFC 8259: unescaped control characters U+0000–U+001F are invalid
                throw source.error("Unescaped control character U+" + String.format("%04X", (int) c) + " in string");
            } else {
                sb.append(c);
            }
        }
    }

    /**
     * Parses an escape sequence after the backslash has been consumed.
     * Returns the character(s) the escape represents.
     */
    private char[] parseEscapeSequence() {
        if (source.isAtEnd()) {
            throw source.error("Unterminated escape sequence");
        }
        char c = source.advance();
        return switch (c) {
            case '"'  -> new char[]{'"'};
            case '\\' -> new char[]{'\\'};
            case '/'  -> new char[]{'/'};
            case 'b'  -> new char[]{'\b'};
            case 'f'  -> new char[]{'\f'};
            case 'n'  -> new char[]{'\n'};
            case 'r'  -> new char[]{'\r'};
            case 't'  -> new char[]{'\t'};
            case 'u'  -> parseUnicodeEscape();
            default   -> throw source.error("Invalid escape sequence '\\" + c + "'");
        };
    }

    /**
     * Parses a backslash-u Unicode escape (the 'u' has already been consumed).
     * Handles surrogate pairs: if the code unit is a high surrogate (U+D800-U+DBFF),
     * expects a following backslash-u low surrogate (U+DC00-U+DFFF).
     */
    private char[] parseUnicodeEscape() {
        int codeUnit = parseHex4();

        if (Character.isHighSurrogate((char) codeUnit)) {
            // Must be followed by a low surrogate escape sequence
            if (source.isAtEnd() || source.peek() != '\\') {
                throw source.error("High surrogate U+" + String.format("%04X", codeUnit)
                        + " must be followed by a low surrogate (\\uDC00-\\uDFFF)");
            }
            source.advance(); // consume '\'
            if (source.isAtEnd() || source.peek() != 'u') {
                throw source.error("High surrogate U+" + String.format("%04X", codeUnit)
                        + " must be followed by \\uXXXX low surrogate");
            }
            source.advance(); // consume 'u'
            int lowUnit = parseHex4();
            if (!Character.isLowSurrogate((char) lowUnit)) {
                throw source.error("Expected low surrogate (U+DC00-U+DFFF) but found U+"
                        + String.format("%04X", lowUnit));
            }
            int codePoint = Character.toCodePoint((char) codeUnit, (char) lowUnit);
            return Character.toChars(codePoint);
        }

        if (Character.isLowSurrogate((char) codeUnit)) {
            throw source.error("Unexpected low surrogate U+" + String.format("%04X", codeUnit)
                    + " without preceding high surrogate");
        }

        return new char[]{(char) codeUnit};
    }

    /**
     * Parses exactly 4 hexadecimal digits and returns the integer value.
     */
    private int parseHex4() {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            if (source.isAtEnd()) {
                throw source.error("Incomplete Unicode escape (expected 4 hex digits)");
            }
            char c = source.advance();
            int digit = hexDigit(c);
            if (digit == -1) {
                throw source.error("Invalid hex digit '" + c + "' in Unicode escape");
            }
            value = (value << 4) | digit;
        }
        return value;
    }

    /**
     * Returns the numeric value of a hex digit, or -1 if not a hex digit.
     */
    private static int hexDigit(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return -1;
    }

    // --- Number parser ---

    /**
     * Parses a JSON number following the RFC 8259 grammar:
     * <pre>
     * number = [ minus ] int [ frac ] [ exp ]
     * minus  = '-'
     * int    = '0' / ( digit1-9 *digit )
     * frac   = '.' 1*digit
     * exp    = ( 'e' / 'E' ) [ '+' / '-' ] 1*digit
     * </pre>
     *
     * <p>The parser slices the matched character sequence and passes the raw
     * string to {@code new JsonNumber(raw)}, preserving the original lexeme
     * for round-trip fidelity.</p>
     */
    private JsonNumber parseNumber() {
        int start = (int) source.offset();

        // Optional minus
        if (!source.isAtEnd() && source.peek() == '-') {
            source.advance();
        }

        // Integer part
        if (source.isAtEnd()) {
            throw source.error("Expected digit after '-'");
        }
        char first = source.peek();
        if (first == '0') {
            source.advance();
            // After '0', must not be followed by another digit (no leading zeros)
            if (!source.isAtEnd()) {
                char next = source.peek();
                if (next >= '0' && next <= '9') {
                    throw source.error("Leading zeros are not allowed in JSON numbers");
                }
            }
        } else if (first >= '1' && first <= '9') {
            source.advance();
            while (!source.isAtEnd() && source.peek() >= '0' && source.peek() <= '9') {
                source.advance();
            }
        } else {
            throw source.error("Expected digit but found '" + first + "'");
        }

        // Optional fractional part
        if (!source.isAtEnd() && source.peek() == '.') {
            source.advance();
            if (source.isAtEnd() || source.peek() < '0' || source.peek() > '9') {
                throw source.error("Expected digit after decimal point");
            }
            while (!source.isAtEnd() && source.peek() >= '0' && source.peek() <= '9') {
                source.advance();
            }
        }

        // Optional exponent part
        if (!source.isAtEnd() && (source.peek() == 'e' || source.peek() == 'E')) {
            source.advance();
            if (!source.isAtEnd() && (source.peek() == '+' || source.peek() == '-')) {
                source.advance();
            }
            if (source.isAtEnd() || source.peek() < '0' || source.peek() > '9') {
                throw source.error("Expected digit in exponent");
            }
            while (!source.isAtEnd() && source.peek() >= '0' && source.peek() <= '9') {
                source.advance();
            }
        }

        int end = (int) source.offset();
        // CharSource tracks offset = pos, and we need the raw substring
        // We access the input indirectly through the start/end offsets
        String raw = source.substring(start, end);
        return new JsonNumber(raw);
    }

    // --- Array parser ---

    /**
     * Parses a JSON array: {@code [ value, value, ... ]}.
     */
    private JsonArray parseArray() {
        source.expect('[');
        incrementDepth();

        source.skipWhitespace();
        if (!source.isAtEnd() && source.peek() == ']') {
            source.advance();
            decrementDepth();
            return new JsonArray(new ArrayList<>());
        }

        List<JsonValue> elements = new ArrayList<>();
        while (true) {
            source.skipWhitespace();
            elements.add(parseValue());
            source.skipWhitespace();

            if (source.isAtEnd()) {
                throw source.error("Unterminated array");
            }
            char c = source.peek();
            if (c == ']') {
                source.advance();
                decrementDepth();
                return new JsonArray(elements);
            }
            if (c == ',') {
                source.advance();
            } else {
                throw source.error("Expected ',' or ']' in array but found '" + c + "'");
            }
        }
    }

    // --- Object parser ---

    /**
     * Parses a JSON object: {@code { "key": value, ... }}.
     *
     * <p>Duplicate keys use last-value-wins semantics, matching Jackson, Gson,
     * Python's {@code json}, and Go's {@code encoding/json}.</p>
     */
    private JsonObject parseObject() {
        source.expect('{');
        incrementDepth();

        source.skipWhitespace();
        if (!source.isAtEnd() && source.peek() == '}') {
            source.advance();
            decrementDepth();
            return new JsonObject(new LinkedHashMap<>());
        }

        Map<String, JsonValue> members = new LinkedHashMap<>();
        while (true) {
            source.skipWhitespace();

            // Key must be a string
            if (source.isAtEnd()) {
                throw source.error("Unterminated object");
            }
            if (source.peek() != '"') {
                throw source.error("Expected string key but found '" + source.peek() + "'");
            }
            String key = parseString().value();

            // Colon separator
            source.skipWhitespace();
            source.expect(':');

            // Value
            source.skipWhitespace();
            JsonValue value = parseValue();

            // Last-value-wins for duplicate keys
            members.put(key, value);

            source.skipWhitespace();
            if (source.isAtEnd()) {
                throw source.error("Unterminated object");
            }
            char c = source.peek();
            if (c == '}') {
                source.advance();
                decrementDepth();
                return new JsonObject(members);
            }
            if (c == ',') {
                source.advance();
            } else {
                throw source.error("Expected ',' or '}' in object but found '" + c + "'");
            }
        }
    }

    // --- Depth tracking ---

    private void incrementDepth() {
        depth++;
        if (depth > MAX_DEPTH) {
            throw source.error("Maximum nesting depth of " + MAX_DEPTH + " exceeded");
        }
    }

    private void decrementDepth() {
        depth--;
    }
}
