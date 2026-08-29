package com.jvalue;

/**
 * Represents a JSON boolean value.
 *
 * <p>This class has only two instances: {@link #TRUE} and {@link #FALSE}.</p>
 */
public final class JsonBoolean implements JsonValue {

    public static final JsonBoolean TRUE = new JsonBoolean(true);
    public static final JsonBoolean FALSE = new JsonBoolean(false);

    private final boolean value;

    private JsonBoolean(boolean value) {
        this.value = value;
    }

    /**
     * Returns the boolean value.
     */
    public boolean value() {
        return value;
    }

    @Override
    public JsonType type() {
        return JsonType.BOOLEAN;
    }

    @Override
    public int hashCode() {
        return Boolean.hashCode(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JsonBoolean other)) return false;
        return this.value == other.value;
    }

    @Override
    public String toString() {
        return Boolean.toString(value);
    }
}
