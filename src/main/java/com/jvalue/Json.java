package com.jvalue;

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
     * Returns the library name and version.
     */
    public static String version() {
        return "JValue 0.1.0";
    }
}
