package com.jvalue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Represents a JSON array.
 *
 * <p>A JSON array is an ordered sequence of zero or more values.
 * Values are immutable once created. The underlying list is exposed as an unmodifiable view.</p>
 */
public final class JsonArray implements JsonValue, Iterable<JsonValue> {

    private final List<JsonValue> elements;

    /**
     * Package-private constructor. The caller MUST NOT retain or modify
     * the provided list after this call — ownership is transferred.
     */
    JsonArray(List<JsonValue> elements) {
        this.elements = Collections.unmodifiableList(elements);
    }

    /**
     * Creates an empty JSON array.
     */
    public static JsonArray empty() {
        return new JsonArray(new ArrayList<>());
    }

    /**
     * Creates a JSON array from the given values.
     */
    public static JsonArray of(JsonValue... values) {
        List<JsonValue> list = new ArrayList<>(values.length);
        for (JsonValue v : values) {
            list.add(Objects.requireNonNull(v));
        }
        return new JsonArray(list);
    }

    /**
     * Returns the number of elements in this array.
     */
    public int size() {
        return elements.size();
    }

    /**
     * Returns true if this array contains no elements.
     */
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    /**
     * Returns the JSON value at the specified index.
     * @throws IndexOutOfBoundsException if the index is out of range.
     */
    public JsonValue get(int index) {
        return elements.get(index);
    }

    /**
     * Returns the string value at the specified index.
     * @throws IndexOutOfBoundsException if the index is out of range.
     * @throws ClassCastException if the value is not a JSON string.
     */
    public String getString(int index) {
        return get(index).asString();
    }

    /**
     * Returns the boolean value at the specified index.
     * @throws IndexOutOfBoundsException if the index is out of range.
     * @throws ClassCastException if the value is not a JSON boolean.
     */
    public boolean getBoolean(int index) {
        return get(index).asBoolean();
    }

    /**
     * Returns the int value at the specified index.
     * @throws IndexOutOfBoundsException if the index is out of range.
     * @throws ClassCastException if the value is not a JSON number.
     */
    public int getInt(int index) {
        return get(index).asInt();
    }

    /**
     * Returns the double value at the specified index.
     * @throws IndexOutOfBoundsException if the index is out of range.
     * @throws ClassCastException if the value is not a JSON number.
     */
    public double getDouble(int index) {
        return get(index).asDouble();
    }

    /**
     * Returns the JSON object at the specified index.
     * @throws IndexOutOfBoundsException if the index is out of range.
     * @throws ClassCastException if the value is not a JSON object.
     */
    public JsonObject getObject(int index) {
        return get(index).asObject();
    }

    /**
     * Returns the JSON array at the specified index.
     * @throws IndexOutOfBoundsException if the index is out of range.
     * @throws ClassCastException if the value is not a JSON array.
     */
    public JsonArray getArray(int index) {
        return get(index).asArray();
    }

    /**
     * Returns an unmodifiable view of the underlying list.
     */
    public List<JsonValue> asList() {
        return elements;
    }

    @Override
    public Iterator<JsonValue> iterator() {
        return elements.iterator();
    }

    @Override
    public JsonType type() {
        return JsonType.ARRAY;
    }

    @Override
    public int hashCode() {
        return elements.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JsonArray other)) return false;
        return this.elements.equals(other.elements);
    }

    @Override
    public String toString() {
        return elements.toString();
    }
}
