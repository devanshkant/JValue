package com.jvalue;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents a JSON number.
 *
 * <p>JSON numbers are arbitrarily precise. This class stores the original
 * string representation from the JSON input to prevent precision loss.
 * Convenience methods are provided to convert the value to common Java types.</p>
 *
 * <h2>Equality semantics</h2>
 *
 * <p>JValue deliberately defines {@code JsonNumber} equality as equality of the
 * stored JSON representation (the raw lexical form). This means that {@code "1"},
 * {@code "1.0"}, and {@code "1e0"} are considered <em>distinct</em> values, and
 * {@code "0"} and {@code "-0"} are also distinct.</p>
 *
 * <p>This design preserves round-trip fidelity: parsing and re-serializing a JSON
 * document will produce the exact numeric spellings from the original input.
 * If mathematical comparison is needed, use {@link #asBigDecimal()} explicitly:</p>
 *
 * <pre>{@code
 * boolean mathEqual = a.asBigDecimal().compareTo(b.asBigDecimal()) == 0;
 * }</pre>
 */
public final class JsonNumber implements JsonValue {

    private final String raw;

    /**
     * Package-private constructor. The caller MUST provide a valid, non-empty
     * numeric string. The parser is responsible for validating JSON number grammar
     * before calling this constructor.
     *
     * @throws NullPointerException if raw is null
     * @throws IllegalArgumentException if raw is empty
     */
    JsonNumber(String raw) {
        Objects.requireNonNull(raw, "Number value cannot be null");
        if (raw.isEmpty()) {
            throw new IllegalArgumentException("Number value cannot be empty");
        }
        this.raw = raw;
    }

    /**
     * Creates a JSON number from its raw string representation.
     *
     * <p>This factory accepts any non-null, non-empty string. It does not validate
     * that the string conforms to the JSON number grammar — that responsibility
     * belongs to the parser. This factory is intended for advanced use cases
     * such as constructing numbers from known-valid representations.</p>
     *
     * @param raw the raw numeric string (e.g. "42", "3.14", "1e2")
     * @throws NullPointerException if raw is null
     * @throws IllegalArgumentException if raw is empty
     */
    public static JsonNumber ofRaw(String raw) {
        return new JsonNumber(raw);
    }

    /**
     * Returns the raw string representation of the number as it appeared
     * in the JSON input. This is the authoritative representation used by
     * the serializer for round-trip fidelity.
     */
    public String raw() {
        return raw;
    }

    @Override
    public int asInt() {
        return new BigDecimal(raw).intValueExact();
    }

    @Override
    public long asLong() {
        return new BigDecimal(raw).longValueExact();
    }

    /**
     * Returns the value as a {@code double}.
     *
     * <p>This is an explicit, potentially lossy conversion. Values outside
     * the range of {@code double} will return {@link Double#POSITIVE_INFINITY}
     * or {@link Double#NEGATIVE_INFINITY}. Values with more than ~15 significant
     * digits will be rounded. Use {@link #asBigDecimal()} for exact precision.</p>
     */
    @Override
    public double asDouble() {
        return Double.parseDouble(raw);
    }

    /**
     * Returns the value as a {@link BigDecimal} for exact precision operations
     * and explicit mathematical comparison.
     *
     * <p>This is the recommended method for comparing JSON numbers by mathematical
     * value rather than by lexical representation:</p>
     *
     * <pre>{@code
     * boolean mathEqual = a.asBigDecimal().compareTo(b.asBigDecimal()) == 0;
     * }</pre>
     */
    public BigDecimal asBigDecimal() {
        return new BigDecimal(raw);
    }

    @Override
    public JsonType type() {
        return JsonType.NUMBER;
    }

    /**
     * Hash code based on the raw lexical representation.
     * Consistent with {@link #equals(Object)}.
     */
    @Override
    public int hashCode() {
        return raw.hashCode();
    }

    /**
     * Equality based on the raw lexical representation.
     *
     * <p>{@code "1"}, {@code "1.0"}, and {@code "1e0"} are considered distinct.
     * For mathematical comparison, use {@link #asBigDecimal()} with
     * {@link BigDecimal#compareTo(BigDecimal)}.</p>
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JsonNumber other)) return false;
        return this.raw.equals(other.raw);
    }

    /**
     * Returns the raw string representation of this number.
     * This is the same value returned by {@link #raw()}.
     */
    @Override
    public String toString() {
        return raw;
    }
}
