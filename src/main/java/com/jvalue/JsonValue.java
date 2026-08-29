package com.jvalue;

/**
 * Represents a JSON value as defined by RFC 8259.
 *
 * <p>This is the root of a sealed hierarchy. All JSON values are immutable.
 * The concrete implementations are {@link JsonObject}, {@link JsonArray},
 * {@link JsonString}, {@link JsonNumber}, {@link JsonBoolean}, and {@link JsonNull}.</p>
 */
public sealed interface JsonValue permits 
    JsonObject, JsonArray, JsonString, JsonNumber, JsonBoolean, JsonNull {

    /**
     * Returns the type of this JSON value.
     */
    JsonType type();

    /**
     * Returns true if this value is a JSON object.
     */
    default boolean isObject() {
        return type() == JsonType.OBJECT;
    }

    /**
     * Returns true if this value is a JSON array.
     */
    default boolean isArray() {
        return type() == JsonType.ARRAY;
    }

    /**
     * Returns true if this value is a JSON string.
     */
    default boolean isString() {
        return type() == JsonType.STRING;
    }

    /**
     * Returns true if this value is a JSON number.
     */
    default boolean isNumber() {
        return type() == JsonType.NUMBER;
    }

    /**
     * Returns true if this value is a JSON boolean.
     */
    default boolean isBoolean() {
        return type() == JsonType.BOOLEAN;
    }

    /**
     * Returns true if this value is a JSON null.
     */
    default boolean isNull() {
        return type() == JsonType.NULL;
    }

    /**
     * Casts this value to a {@link JsonObject}.
     * @throws ClassCastException if this value is not a JSON object.
     */
    default JsonObject asObject() {
        return (JsonObject) this;
    }

    /**
     * Casts this value to a {@link JsonArray}.
     * @throws ClassCastException if this value is not a JSON array.
     */
    default JsonArray asArray() {
        return (JsonArray) this;
    }

    /**
     * Casts this value to a {@link JsonString}.
     * @throws ClassCastException if this value is not a JSON string.
     */
    default JsonString asJsonString() {
        return (JsonString) this;
    }

    /**
     * Casts this value to a {@link JsonNumber}.
     * @throws ClassCastException if this value is not a JSON number.
     */
    default JsonNumber asJsonNumber() {
        return (JsonNumber) this;
    }

    /**
     * Returns the boolean value if this is a {@link JsonBoolean}.
     * @throws ClassCastException if this value is not a JSON boolean.
     */
    default boolean asBoolean() {
        return ((JsonBoolean) this).value();
    }

    /**
     * Returns the string value if this is a {@link JsonString}.
     * @throws ClassCastException if this value is not a JSON string.
     */
    default String asString() {
        return asJsonString().value();
    }

    /**
     * Returns the int value if this is a {@link JsonNumber}.
     * @throws ClassCastException if this value is not a JSON number.
     */
    default int asInt() {
        return asJsonNumber().asInt();
    }

    /**
     * Returns the long value if this is a {@link JsonNumber}.
     * @throws ClassCastException if this value is not a JSON number.
     */
    default long asLong() {
        return asJsonNumber().asLong();
    }

    /**
     * Returns the double value if this is a {@link JsonNumber}.
     * @throws ClassCastException if this value is not a JSON number.
     */
    default double asDouble() {
        return asJsonNumber().asDouble();
    }

    /**
     * Returns the JSON null singleton.
     */
    static JsonNull ofNull() {
        return JsonNull.INSTANCE;
    }

    /**
     * Returns a JSON boolean.
     */
    static JsonBoolean of(boolean value) {
        return value ? JsonBoolean.TRUE : JsonBoolean.FALSE;
    }

    /**
     * Returns a JSON string. Throws if the value is null.
     */
    static JsonString ofString(String value) {
        return new JsonString(java.util.Objects.requireNonNull(value, "Use ofNull() for JSON null"));
    }

    /**
     * Returns a JSON string, or JSON null if the string is null.
     */
    static JsonValue of(String value) {
        return value == null ? JsonNull.INSTANCE : new JsonString(value);
    }

    /**
     * Returns a JSON number from an int.
     */
    static JsonNumber of(int value) {
        return new JsonNumber(Integer.toString(value));
    }

    /**
     * Returns a JSON number from a long.
     */
    static JsonNumber of(long value) {
        return new JsonNumber(Long.toString(value));
    }

    /**
     * Returns a JSON number from a double.
     * @throws IllegalArgumentException if the double is NaN or Infinity.
     */
    static JsonNumber of(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("JSON numbers cannot be NaN or Infinity");
        }
        return new JsonNumber(Double.toString(value));
    }
}
