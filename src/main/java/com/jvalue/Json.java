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
     * Returns the library name and version.
     */
    public static String version() {
        return "JValue 0.1.0";
    }
}
