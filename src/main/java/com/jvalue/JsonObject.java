package com.jvalue;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a JSON object.
 *
 * <p>A JSON object is an unordered collection of zero or more name/value pairs,
 * where a name is a string and a value is a string, number, boolean, null, object, or array.
 * However, this implementation preserves insertion order for predictable serialization.</p>
 *
 * <p>Values are immutable once created. The underlying map is exposed as an unmodifiable view.</p>
 */
public final class JsonObject implements JsonValue, Iterable<Map.Entry<String, JsonValue>> {

    private final Map<String, JsonValue> members;

    /**
     * Package-private constructor. The caller MUST NOT retain or modify
     * the provided map after this call — ownership is transferred.
     */
    JsonObject(Map<String, JsonValue> members) {
        this.members = Collections.unmodifiableMap(members);
    }

    /**
     * Creates an empty JSON object.
     */
    public static JsonObject empty() {
        return new JsonObject(new LinkedHashMap<>());
    }

    /**
     * Creates a JSON object from the given key-value pairs.
     */
    public static JsonObject of(String k1, JsonValue v1) {
        Map<String, JsonValue> map = new LinkedHashMap<>();
        map.put(Objects.requireNonNull(k1), Objects.requireNonNull(v1));
        return new JsonObject(map);
    }
    
    public static JsonObject of(String k1, JsonValue v1, String k2, JsonValue v2) {
        Map<String, JsonValue> map = new LinkedHashMap<>();
        map.put(Objects.requireNonNull(k1), Objects.requireNonNull(v1));
        map.put(Objects.requireNonNull(k2), Objects.requireNonNull(v2));
        return new JsonObject(map);
    }

    /**
     * Returns the number of key-value pairs in this object.
     */
    public int size() {
        return members.size();
    }

    /**
     * Returns true if this object contains no key-value pairs.
     */
    public boolean isEmpty() {
        return members.isEmpty();
    }

    /**
     * Returns true if this object contains the specified key.
     */
    public boolean has(String key) {
        return members.containsKey(key);
    }

    /**
     * Returns the JSON value associated with the specified key, or null if it does not exist.
     */
    public JsonValue get(String key) {
        return members.get(key);
    }

    /**
     * Returns the string value associated with the specified key.
     * @throws NullPointerException if the key does not exist.
     * @throws ClassCastException if the value is not a JSON string.
     */
    public String getString(String key) {
        return getRequired(key).asString();
    }

    /**
     * Returns the boolean value associated with the specified key.
     * @throws NullPointerException if the key does not exist.
     * @throws ClassCastException if the value is not a JSON boolean.
     */
    public boolean getBoolean(String key) {
        return getRequired(key).asBoolean();
    }

    /**
     * Returns the int value associated with the specified key.
     * @throws NullPointerException if the key does not exist.
     * @throws ClassCastException if the value is not a JSON number.
     * @throws NumberFormatException if the number is not a valid int.
     */
    public int getInt(String key) {
        return getRequired(key).asInt();
    }

    /**
     * Returns the double value associated with the specified key.
     * @throws NullPointerException if the key does not exist.
     * @throws ClassCastException if the value is not a JSON number.
     */
    public double getDouble(String key) {
        return getRequired(key).asDouble();
    }

    /**
     * Returns the JSON object associated with the specified key.
     * @throws NullPointerException if the key does not exist.
     * @throws ClassCastException if the value is not a JSON object.
     */
    public JsonObject getObject(String key) {
        return getRequired(key).asObject();
    }

    /**
     * Returns the JSON array associated with the specified key.
     * @throws NullPointerException if the key does not exist.
     * @throws ClassCastException if the value is not a JSON array.
     */
    public JsonArray getArray(String key) {
        return getRequired(key).asArray();
    }

    private JsonValue getRequired(String key) {
        JsonValue value = members.get(key);
        if (value == null) {
            throw new NoSuchElementException("Missing required key: " + key);
        }
        return value;
    }

    /**
     * Returns an unmodifiable set of the keys in this object.
     */
    public Set<String> keys() {
        return members.keySet();
    }

    /**
     * Returns an unmodifiable view of the underlying map.
     */
    public Map<String, JsonValue> asMap() {
        return members;
    }

    @Override
    public Iterator<Map.Entry<String, JsonValue>> iterator() {
        return members.entrySet().iterator();
    }

    @Override
    public JsonType type() {
        return JsonType.OBJECT;
    }

    @Override
    public int hashCode() {
        return members.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JsonObject other)) return false;
        return this.members.equals(other.members);
    }

    @Override
    public String toString() {
        return members.toString();
    }
}
