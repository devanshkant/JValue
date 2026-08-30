package com.jvalue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * JValue — Zero-Dependency JSON Toolkit for Java 25.
 *
 * <p>This is the primary entry point for parsing and serializing JSON.
 * All methods in this class are static convenience methods that delegate
 * to the internal parser and serializer.</p>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * JsonValue value = Json.parse("{\"name\": \"JValue\"}");
 * String name = value.asObject().getString("name");
 * }</pre>
 *
 * @see JsonValue
 */
public final class Json {

    private Json() {
        // Static utility class — no instantiation
    }

    /**
     * Parses a JSON string into a {@link JsonValue}.
     *
     * <p>Accepts any valid JSON value as the root: objects, arrays, strings,
     * numbers, booleans, and null are all valid JSON documents per RFC 8259.</p>
     *
     * @param json the JSON text to parse
     * @return the parsed JSON value
     * @throws JsonParseException if the input is not valid JSON
     * @throws NullPointerException if json is null
     */
    public static JsonValue parse(String json) {
        CharSource source = new CharSource(json);
        JsonParser parser = new JsonParser(source);
        return parser.parseDocument();
    }

    /**
     * Reads a UTF-8 JSON file into a {@link JsonValue}.
     *
     * <p>This is a convenience wrapper around {@link Files#readString(Path, Charset)}
     * and {@link #parse(String)}. It preserves the parser's normal input policy.</p>
     *
     * @param path the file to read
     * @return the parsed JSON value
     * @throws IOException if reading fails
     * @throws JsonParseException if the file content is not valid JSON
     * @throws NullPointerException if path is null
     */
    public static JsonValue read(Path path) throws IOException {
        return read(path, StandardCharsets.UTF_8);
    }

    /**
     * Reads a JSON file with the given charset into a {@link JsonValue}.
     *
     * <p>This is a convenience wrapper around {@link Files#readString(Path, Charset)}
     * and {@link #parse(String)}. It preserves the parser's normal input policy.</p>
     *
     * @param path the file to read
     * @param charset the charset used to decode the file
     * @return the parsed JSON value
     * @throws IOException if reading fails
     * @throws JsonParseException if the file content is not valid JSON
     * @throws NullPointerException if path or charset is null
     */
    public static JsonValue read(Path path, Charset charset) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(charset, "charset");
        return parse(Files.readString(path, charset));
    }

    /**
     * Serializes a JSON value to compact JSON text.
     *
     * @param value the value to serialize
     * @return compact JSON text
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if the value contains invalid internal state
     */
    public static String stringify(JsonValue value) {
        return JsonSerializer.serialize(value);
    }

    /**
     * Writes a JSON value as compact JSON text to an {@link Appendable}.
     *
     * @param value the value to serialize
     * @param out the destination appendable
     * @throws IOException if the appendable fails while writing
     * @throws NullPointerException if value or out is null
     * @throws IllegalArgumentException if the value contains invalid internal state
     */
    public static void write(JsonValue value, Appendable out) throws IOException {
        JsonSerializer.write(value, out);
    }

    /**
     * Writes a JSON value as compact UTF-8 JSON text to a file.
     *
     * <p>This is a convenience wrapper around {@link Files#newBufferedWriter(Path, Charset)}
     * and {@link #write(JsonValue, Appendable)}.</p>
     *
     * @param value the value to serialize
     * @param path the file to write
     * @throws IOException if writing fails
     * @throws NullPointerException if value or path is null
     * @throws IllegalArgumentException if the value contains invalid internal state
     */
    public static void writeFile(JsonValue value, Path path) throws IOException {
        writeFile(value, path, StandardCharsets.UTF_8);
    }

    /**
     * Writes a JSON value as compact JSON text to a file with the given charset.
     *
     * <p>This is a convenience wrapper around {@link Files#newBufferedWriter(Path, Charset)}
     * and {@link #write(JsonValue, Appendable)}.</p>
     *
     * @param value the value to serialize
     * @param path the file to write
     * @param charset the charset used to encode the file
     * @throws IOException if writing fails
     * @throws NullPointerException if value, path, or charset is null
     * @throws IllegalArgumentException if the value contains invalid internal state
     */
    public static void writeFile(JsonValue value, Path path, Charset charset) throws IOException {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(charset, "charset");
        try (var out = Files.newBufferedWriter(path, charset)) {
            write(value, out);
        }
    }

    /**
     * Serializes a JSON value to human-readable JSON text.
     *
     * @param value the value to serialize
     * @return pretty-printed JSON text with two-space indentation
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if the value contains invalid internal state
     */
    public static String stringifyPretty(JsonValue value) {
        return JsonSerializer.serializePretty(value);
    }

    /**
     * Writes a JSON value as human-readable JSON text to an {@link Appendable}.
     *
     * @param value the value to serialize
     * @param out the destination appendable
     * @throws IOException if the appendable fails while writing
     * @throws NullPointerException if value or out is null
     * @throws IllegalArgumentException if the value contains invalid internal state
     */
    public static void writePretty(JsonValue value, Appendable out) throws IOException {
        JsonSerializer.writePretty(value, out);
    }

    /**
     * Writes a JSON value as pretty UTF-8 JSON text to a file.
     *
     * <p>This is a convenience wrapper around {@link Files#newBufferedWriter(Path, Charset)}
     * and {@link #writePretty(JsonValue, Appendable)}.</p>
     *
     * @param value the value to serialize
     * @param path the file to write
     * @throws IOException if writing fails
     * @throws NullPointerException if value or path is null
     * @throws IllegalArgumentException if the value contains invalid internal state
     */
    public static void writePrettyFile(JsonValue value, Path path) throws IOException {
        writePrettyFile(value, path, StandardCharsets.UTF_8);
    }

    /**
     * Writes a JSON value as pretty JSON text to a file with the given charset.
     *
     * <p>This is a convenience wrapper around {@link Files#newBufferedWriter(Path, Charset)}
     * and {@link #writePretty(JsonValue, Appendable)}.</p>
     *
     * @param value the value to serialize
     * @param path the file to write
     * @param charset the charset used to encode the file
     * @throws IOException if writing fails
     * @throws NullPointerException if value, path, or charset is null
     * @throws IllegalArgumentException if the value contains invalid internal state
     */
    public static void writePrettyFile(JsonValue value, Path path, Charset charset) throws IOException {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(charset, "charset");
        try (var out = Files.newBufferedWriter(path, charset)) {
            writePretty(value, out);
        }
    }

    /**
     * Resolves a JSON Pointer against a JSON value tree.
     *
     * @param root the root JSON value
     * @param pointer the JSON Pointer string
     * @return the value addressed by the pointer
     * @throws NullPointerException if root or pointer is null
     * @throws IllegalArgumentException if pointer is not valid RFC 6901 syntax
     * @throws java.util.NoSuchElementException if the pointer does not resolve
     */
    public static JsonValue pointer(JsonValue root, String pointer) {
        Objects.requireNonNull(root, "root");
        return JsonPointer.compile(pointer).query(root);
    }

    /**
     * Resolves a JSON Pointer against a JSON value tree.
     *
     * @param root the root JSON value
     * @param pointer the JSON Pointer string
     * @return the addressed value, or empty if the valid pointer does not resolve
     * @throws NullPointerException if root or pointer is null
     * @throws IllegalArgumentException if pointer is not valid RFC 6901 syntax
     */
    public static Optional<JsonValue> pointerOptional(JsonValue root, String pointer) {
        Objects.requireNonNull(root, "root");
        return JsonPointer.compile(pointer).queryOptional(root);
    }

    /**
     * Returns the library name and version.
     */
    public static String version() {
        return "JValue 0.1.0";
    }
}
