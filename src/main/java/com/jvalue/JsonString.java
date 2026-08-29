package com.jvalue;

import java.util.Objects;

/**
 * Represents a JSON string.
 */
public final class JsonString implements JsonValue {

    private final String value;

    JsonString(String value) {
        this.value = Objects.requireNonNull(value, "String value cannot be null");
    }

    /**
     * Returns the string value.
     */
    public String value() {
        return value;
    }

    @Override
    public JsonType type() {
        return JsonType.STRING;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JsonString other)) return false;
        return this.value.equals(other.value);
    }

    @Override
    public String toString() {
        // Returns the raw string value, not JSON-encoded output.
        // JSON serialization (with escaping and quoting) is handled by the serializer.
        return value;
    }
}
