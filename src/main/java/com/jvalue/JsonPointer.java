package com.jvalue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * An immutable compiled JSON Pointer as defined by RFC 6901.
 *
 * <p>A compiled pointer stores decoded reference tokens and can query a
 * {@link JsonValue} tree without reparsing the original pointer string.</p>
 */
public final class JsonPointer {

    private final String pointer;
    private final List<String> tokens;

    private JsonPointer(String pointer, List<String> tokens) {
        this.pointer = pointer;
        this.tokens = List.copyOf(tokens);
    }

    /**
     * Compiles a JSON Pointer string and decodes its reference tokens.
     *
     * <p>The empty string points at the root value and therefore has no tokens.
     * Any non-empty pointer must begin with {@code /}. Within a token, RFC 6901
     * escapes are decoded as {@code ~0 -> ~} and {@code ~1 -> /}.</p>
     *
     * @param pointer the JSON Pointer string
     * @return an immutable compiled pointer
     * @throws NullPointerException if pointer is null
     * @throws IllegalArgumentException if pointer is not valid RFC 6901 syntax
     */
    public static JsonPointer compile(String pointer) {
        Objects.requireNonNull(pointer, "pointer");

        if (pointer.isEmpty()) {
            return new JsonPointer(pointer, List.of());
        }
        if (pointer.charAt(0) != '/') {
            throw new IllegalArgumentException("JSON Pointer must be empty or start with '/'");
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();

        for (int i = 1; i < pointer.length(); i++) {
            char c = pointer.charAt(i);
            if (c == '/') {
                tokens.add(token.toString());
                token.setLength(0);
            } else if (c == '~') {
                if (i + 1 >= pointer.length()) {
                    throw new IllegalArgumentException("Invalid JSON Pointer escape: '~' must be followed by '0' or '1'");
                }
                char escaped = pointer.charAt(++i);
                switch (escaped) {
                    case '0' -> token.append('~');
                    case '1' -> token.append('/');
                    default -> throw new IllegalArgumentException(
                            "Invalid JSON Pointer escape: '~" + escaped + "'");
                }
            } else {
                token.append(c);
            }
        }

        tokens.add(token.toString());
        return new JsonPointer(pointer, tokens);
    }

    /**
     * Returns the decoded reference tokens for this pointer.
     */
    public List<String> tokens() {
        return tokens;
    }

    /**
     * Resolves this pointer against a JSON value tree.
     *
     * @param root the root JSON value
     * @return the value addressed by this pointer
     * @throws NullPointerException if root is null
     * @throws NoSuchElementException if this valid pointer does not resolve
     */
    public JsonValue query(JsonValue root) {
        Objects.requireNonNull(root, "root");
        JsonValue value = resolve(root);
        if (value == null) {
            throw new NoSuchElementException("JSON Pointer does not resolve: " + pointer);
        }
        return value;
    }

    /**
     * Resolves this pointer against a JSON value tree.
     *
     * @param root the root JSON value
     * @return the addressed value, or empty if this valid pointer does not resolve
     * @throws NullPointerException if root is null
     */
    public Optional<JsonValue> queryOptional(JsonValue root) {
        Objects.requireNonNull(root, "root");
        return Optional.ofNullable(resolve(root));
    }

    private JsonValue resolve(JsonValue root) {
        JsonValue current = root;
        for (String token : tokens) {
            if (current instanceof JsonObject object) {
                current = object.get(token);
                if (current == null) {
                    return null;
                }
            } else if (current instanceof JsonArray array) {
                int index = parseArrayIndex(token);
                if (index < 0 || index >= array.size()) {
                    return null;
                }
                current = array.get(index);
            } else {
                return null;
            }
        }
        return current;
    }

    private static int parseArrayIndex(String token) {
        if (token.isEmpty()) {
            return -1;
        }
        if (token.length() > 1 && token.charAt(0) == '0') {
            return -1;
        }

        int value = 0;
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            int digit = c - '0';
            if (value > (Integer.MAX_VALUE - digit) / 10) {
                return -1;
            }
            value = value * 10 + digit;
        }
        return value;
    }

    @Override
    public String toString() {
        return pointer;
    }

    @Override
    public int hashCode() {
        return pointer.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JsonPointer other)) return false;
        return pointer.equals(other.pointer);
    }
}
