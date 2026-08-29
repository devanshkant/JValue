package com.jvalue;

/**
 * Represents a JSON null value.
 *
 * <p>This is a singleton class. Use {@link JsonNull#INSTANCE} or {@link JsonValue#ofNull()}.</p>
 */
public final class JsonNull implements JsonValue {

    /**
     * The singleton instance of JSON null.
     */
    public static final JsonNull INSTANCE = new JsonNull();

    private JsonNull() {
        // Singleton
    }

    @Override
    public JsonType type() {
        return JsonType.NULL;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof JsonNull;
    }

    @Override
    public String toString() {
        return "null";
    }
}
