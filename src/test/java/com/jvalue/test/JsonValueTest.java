package com.jvalue.test;

import com.jvalue.JsonArray;
import com.jvalue.JsonBoolean;
import com.jvalue.JsonNull;
import com.jvalue.JsonNumber;
import com.jvalue.JsonObject;
import com.jvalue.JsonString;
import com.jvalue.JsonType;
import com.jvalue.JsonValue;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

import static com.jvalue.test.TestRunner.*;

public final class JsonValueTest {

    public static void runAll() {
        runSuite("JsonValue Core", () -> {
            runTest("Type inspection", JsonValueTest::testTypeInspection);
            runTest("Null singleton", JsonValueTest::testNull);
            runTest("Boolean singletons", JsonValueTest::testBoolean);
            runTest("Number basic values", JsonValueTest::testNumber);
            runTest("Number raw preservation", JsonValueTest::testNumberRawPreservation);
            runTest("Number lexical equality", JsonValueTest::testNumberLexicalEquality);
            runTest("Number hashCode consistency", JsonValueTest::testNumberHashCodeConsistency);
            runTest("Number negative zero distinction", JsonValueTest::testNumberNegativeZeroDistinction);
            runTest("Number BigDecimal precision", JsonValueTest::testNumberBigDecimalPrecision);
            runTest("Number double lossy conversion", JsonValueTest::testNumberDoubleLossyConversion);
            runTest("Number exponential notation", JsonValueTest::testNumberExponentialNotation);
            runTest("Number large values", JsonValueTest::testNumberLargeValues);
            runTest("Number input validation", JsonValueTest::testNumberInputValidation);
            runTest("String values", JsonValueTest::testString);
            runTest("Array operations", JsonValueTest::testArray);
            runTest("Array equality is order-sensitive", JsonValueTest::testArrayOrderSensitive);
            runTest("Object operations", JsonValueTest::testObject);
            runTest("Object key with JSON null value", JsonValueTest::testObjectNullValue);
            runTest("Object equality is insertion-order-independent", JsonValueTest::testObjectEqualityOrderIndependent);
            runTest("Immutability", JsonValueTest::testImmutability);
            runTest("Cross-type equality", JsonValueTest::testCrossTypeEquality);
            runTest("Nested structure equality", JsonValueTest::testNestedStructureEquality);
            runTest("Casting throws correctly", JsonValueTest::testCastingThrows);
            runTest("ofString factory", JsonValueTest::testOfStringFactory);
        });
    }

    private static void testTypeInspection() {
        assertTrue(JsonValue.ofNull().isNull());
        assertTrue(JsonValue.of(true).isBoolean());
        assertTrue(JsonValue.of(42).isNumber());
        assertTrue(JsonValue.of("hello").isString());
        assertTrue(JsonArray.empty().isArray());
        assertTrue(JsonObject.empty().isObject());

        assertFalse(JsonValue.ofNull().isObject());

        assertEquals(JsonType.NULL, JsonValue.ofNull().type());
        assertEquals(JsonType.BOOLEAN, JsonValue.of(false).type());
        assertEquals(JsonType.NUMBER, JsonValue.of(3.14).type());
        assertEquals(JsonType.STRING, JsonValue.of("").type());
        assertEquals(JsonType.ARRAY, JsonArray.empty().type());
        assertEquals(JsonType.OBJECT, JsonObject.empty().type());
    }

    private static void testNull() {
        JsonValue n1 = JsonValue.ofNull();
        JsonValue n2 = JsonNull.INSTANCE;
        assertTrue(n1 == n2); // Singleton
        assertEquals(n1, n2);
        assertEquals("null", n1.toString());
        assertFalse(n1.equals(null)); // Java null, not JSON null
    }

    private static void testBoolean() {
        JsonValue t1 = JsonValue.of(true);
        JsonValue t2 = JsonBoolean.TRUE;
        assertTrue(t1 == t2);
        assertTrue(t1.asBoolean());

        JsonValue f1 = JsonValue.of(false);
        assertFalse(f1.asBoolean());

        assertFalse(t1.equals(f1));
        assertEquals("true", t1.toString());
        assertEquals("false", f1.toString());
    }

    // --- JsonNumber tests ---

    private static void testNumber() {
        JsonValue num = JsonValue.of(42);
        assertEquals(42, num.asInt());
        assertEquals(42L, num.asLong());
        assertEquals(42.0, num.asDouble());
        assertEquals("42", num.toString());
        assertEquals("42", num.asJsonNumber().raw());

        JsonValue decimal = JsonValue.of(3.14);
        assertEquals(3.14, decimal.asDouble());
        assertEquals("3.14", decimal.toString());

        // Same factory produces equal values
        assertEquals(JsonValue.of(42), JsonValue.of(42));

        // NaN and Infinity rejected
        assertThrows(IllegalArgumentException.class, () -> JsonValue.of(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> JsonValue.of(Double.POSITIVE_INFINITY));
    }

    /**
     * Tests that raw() and toString() preserve the exact lexical form
     * for all specified edge cases.
     */
    private static void testNumberRawPreservation() {
        String[] cases = {
            "0", "-0", "1", "1.0", "1.00", "1e0", "1E0", "1e+0",
            "0.1", "0.10", "1.234567890123456789",
            "9007199254740993", "1e308", "1e309", "1e-308"
        };
        for (String c : cases) {
            JsonNumber n = JsonNumber.ofRaw(c);
            assertEquals("raw() for " + c, c, n.raw());
            assertEquals("toString() for " + c, c, n.toString());
        }
    }

    /**
     * Tests that lexical equality distinguishes different string representations
     * of mathematically equal values.
     */
    private static void testNumberLexicalEquality() {
        // Same lexical form → equal
        assertEquals(JsonNumber.ofRaw("1"), JsonNumber.ofRaw("1"));
        assertEquals(JsonNumber.ofRaw("1.0"), JsonNumber.ofRaw("1.0"));
        assertEquals(JsonNumber.ofRaw("1e0"), JsonNumber.ofRaw("1e0"));

        // Different lexical form → not equal, even if mathematically equivalent
        assertFalse(JsonNumber.ofRaw("1").equals(JsonNumber.ofRaw("1.0")));
        assertFalse(JsonNumber.ofRaw("1").equals(JsonNumber.ofRaw("1e0")));
        assertFalse(JsonNumber.ofRaw("1").equals(JsonNumber.ofRaw("1.00")));
        assertFalse(JsonNumber.ofRaw("1.0").equals(JsonNumber.ofRaw("1.00")));
        assertFalse(JsonNumber.ofRaw("1e0").equals(JsonNumber.ofRaw("1E0")));
        assertFalse(JsonNumber.ofRaw("1e0").equals(JsonNumber.ofRaw("1e+0")));
        assertFalse(JsonNumber.ofRaw("0.1").equals(JsonNumber.ofRaw("0.10")));

        // Negative zero vs zero → not equal
        assertFalse(JsonNumber.ofRaw("0").equals(JsonNumber.ofRaw("-0")));

        // Mathematical comparison is available via BigDecimal
        assertTrue(JsonNumber.ofRaw("1").asBigDecimal().compareTo(
                   JsonNumber.ofRaw("1.0").asBigDecimal()) == 0);
    }

    /**
     * Tests that equal values have equal hash codes, and that
     * lexically distinct values are likely to have distinct hash codes.
     */
    private static void testNumberHashCodeConsistency() {
        // Same lexical form → same hash code
        assertEquals(JsonNumber.ofRaw("42").hashCode(), JsonNumber.ofRaw("42").hashCode());
        assertEquals(JsonNumber.ofRaw("1e0").hashCode(), JsonNumber.ofRaw("1e0").hashCode());
        assertEquals(JsonNumber.ofRaw("-0").hashCode(), JsonNumber.ofRaw("-0").hashCode());

        // Distinct lexical forms are likely (not guaranteed) to have distinct hash codes.
        // We test a few well-known cases where String.hashCode differs.
        // This is a probabilistic check, not a contract requirement.
        assertTrue("0 vs -0 hashCode should differ",
            JsonNumber.ofRaw("0").hashCode() != JsonNumber.ofRaw("-0").hashCode());
        assertTrue("1 vs 1.0 hashCode should differ",
            JsonNumber.ofRaw("1").hashCode() != JsonNumber.ofRaw("1.0").hashCode());
    }

    /**
     * Tests that -0 and 0 are distinguishable as JSON values,
     * while BigDecimal mathematical comparison treats them as equal.
     */
    private static void testNumberNegativeZeroDistinction() {
        JsonNumber negZero = JsonNumber.ofRaw("-0");
        JsonNumber posZero = JsonNumber.ofRaw("0");

        // Lexically distinct
        assertFalse(negZero.equals(posZero));
        assertEquals("-0", negZero.raw());
        assertEquals("0", posZero.raw());

        // Mathematically equal via BigDecimal
        assertTrue(negZero.asBigDecimal().compareTo(posZero.asBigDecimal()) == 0);

        // double preserves negative zero
        assertTrue(Double.doubleToRawLongBits(negZero.asDouble()) != 
                   Double.doubleToRawLongBits(posZero.asDouble()));
    }

    /**
     * Tests that BigDecimal preserves arbitrary precision.
     */
    private static void testNumberBigDecimalPrecision() {
        // 19-digit decimal: beyond double precision, within BigDecimal
        JsonNumber precise = JsonNumber.ofRaw("1.234567890123456789");
        assertEquals(new BigDecimal("1.234567890123456789"), precise.asBigDecimal());

        // 2^53 + 1: not exactly representable as double
        JsonNumber beyondDouble = JsonNumber.ofRaw("9007199254740993");
        assertEquals(new BigDecimal("9007199254740993"), beyondDouble.asBigDecimal());

        // Beyond long range
        JsonNumber beyondLong = JsonNumber.ofRaw("9223372036854775808");
        assertEquals(new BigDecimal("9223372036854775808"), beyondLong.asBigDecimal());
        assertThrows(ArithmeticException.class, () -> beyondLong.asLong());
    }

    /**
     * Tests that asDouble() is an explicit, potentially lossy conversion.
     */
    private static void testNumberDoubleLossyConversion() {
        // Precision loss: 2^53 + 1 rounds to 2^53
        JsonNumber n = JsonNumber.ofRaw("9007199254740993");
        assertEquals(9007199254740992.0, n.asDouble()); // silently rounded

        // Overflow: 1e309 exceeds Double.MAX_VALUE
        JsonNumber overflow = JsonNumber.ofRaw("1e309");
        assertEquals(Double.POSITIVE_INFINITY, overflow.asDouble());

        // Near-zero: 1e-308 is representable
        JsonNumber nearZero = JsonNumber.ofRaw("1e-308");
        assertTrue(nearZero.asDouble() > 0.0);
        assertTrue(nearZero.asDouble() < 1e-307);

        // Large but representable
        JsonNumber large = JsonNumber.ofRaw("1e308");
        assertTrue(Double.isFinite(large.asDouble()));
    }

    /**
     * Tests exponential notation conversion and raw preservation.
     */
    private static void testNumberExponentialNotation() {
        JsonNumber exp = JsonNumber.ofRaw("1e2");
        assertEquals(100.0, exp.asDouble());
        assertEquals(100, exp.asInt());
        assertEquals(100L, exp.asLong());
        assertEquals("1e2", exp.raw());

        // Case-sensitive lexical distinction
        assertFalse(JsonNumber.ofRaw("1e2").equals(JsonNumber.ofRaw("1E2")));

        // Explicit positive exponent
        JsonNumber expPlus = JsonNumber.ofRaw("1e+0");
        assertEquals(1.0, expPlus.asDouble());
        assertEquals("1e+0", expPlus.raw());

        // Fractional result from exponent
        JsonNumber frac = JsonNumber.ofRaw("1.5e1");
        assertEquals(15.0, frac.asDouble());
        assertEquals(15, frac.asInt());
    }

    /**
     * Tests behavior with values exceeding primitive Java type ranges.
     */
    private static void testNumberLargeValues() {
        // Beyond int and long range
        JsonNumber big = JsonNumber.ofRaw("99999999999999999999");
        assertThrows(ArithmeticException.class, () -> big.asInt());
        assertThrows(ArithmeticException.class, () -> big.asLong());
        assertEquals("99999999999999999999", big.raw());
        assertNotNull(big.asBigDecimal());

        // Beyond long, within BigDecimal
        JsonNumber beyondLong = JsonNumber.ofRaw("9223372036854775808");
        assertThrows(ArithmeticException.class, () -> beyondLong.asLong());
        assertEquals(new BigDecimal("9223372036854775808"), beyondLong.asBigDecimal());
    }

    /**
     * Tests that null and empty inputs are rejected.
     */
    private static void testNumberInputValidation() {
        assertThrows(NullPointerException.class, () -> JsonNumber.ofRaw(null));
        assertThrows(IllegalArgumentException.class, () -> JsonNumber.ofRaw(""));
    }

    // --- Non-number tests (unchanged) ---

    private static void testString() {
        JsonValue str = JsonValue.of("hello");
        assertEquals("hello", str.asString());
        assertEquals("hello", str.toString());
        assertEquals(JsonValue.of("hello"), str);

        // Null string becomes JsonNull
        assertTrue(JsonValue.of((String) null).isNull());
    }

    private static void testArray() {
        JsonArray empty = JsonArray.empty();
        assertEquals(0, empty.size());
        assertTrue(empty.isEmpty());

        JsonArray arr = JsonArray.of(JsonValue.of(1), JsonValue.of("two"), JsonValue.of(true));
        assertEquals(3, arr.size());
        assertEquals(1, arr.getInt(0));
        assertEquals("two", arr.getString(1));
        assertTrue(arr.getBoolean(2));

        assertThrows(IndexOutOfBoundsException.class, () -> arr.get(3));
        assertThrows(ClassCastException.class, () -> arr.getString(0));
    }

    private static void testArrayOrderSensitive() {
        JsonArray a1 = JsonArray.of(JsonValue.of(1), JsonValue.of(2));
        JsonArray a2 = JsonArray.of(JsonValue.of(2), JsonValue.of(1));
        assertFalse(a1.equals(a2));
    }

    private static void testObject() {
        JsonObject empty = JsonObject.empty();
        assertEquals(0, empty.size());
        assertTrue(empty.isEmpty());

        JsonObject obj = JsonObject.of("name", JsonValue.of("Alice"), "age", JsonValue.of(30));
        assertEquals(2, obj.size());
        assertTrue(obj.has("name"));
        assertFalse(obj.has("missing"));

        assertEquals("Alice", obj.getString("name"));
        assertEquals(30, obj.getInt("age"));

        assertNull(obj.get("missing"));
        assertThrows(NoSuchElementException.class, () -> obj.getString("missing"));
    }

    private static void testObjectNullValue() {
        JsonObject obj = JsonObject.of("key", JsonValue.ofNull());
        assertTrue(obj.has("key"));
        assertTrue(obj.get("key").isNull());
    }

    private static void testObjectEqualityOrderIndependent() {
        JsonObject obj1 = JsonObject.of("a", JsonValue.of(1), "b", JsonValue.of(2));
        JsonObject obj2 = JsonObject.of("b", JsonValue.of(2), "a", JsonValue.of(1));
        assertEquals(obj1, obj2);
    }

    private static void testImmutability() {
        JsonArray arr = JsonArray.of(JsonValue.of(1));
        assertThrows(UnsupportedOperationException.class, () -> arr.asList().add(JsonValue.of(2)));

        JsonObject obj = JsonObject.of("k", JsonValue.of("v"));
        assertThrows(UnsupportedOperationException.class, () -> obj.asMap().put("k2", JsonValue.of("v2")));
    }

    private static void testCrossTypeEquality() {
        assertFalse(JsonValue.of(1).equals(JsonValue.of("1")));
        assertFalse(JsonValue.of(true).equals(JsonValue.of("true")));
        assertFalse(JsonValue.ofNull().equals(null));
        assertFalse(JsonValue.of(1).equals(JsonValue.of(true)));
    }

    private static void testNestedStructureEquality() {
        JsonArray nested1 = JsonArray.of(
            JsonObject.of("x", JsonValue.of(1)),
            JsonArray.of(JsonValue.of(true))
        );
        JsonArray nested2 = JsonArray.of(
            JsonObject.of("x", JsonValue.of(1)),
            JsonArray.of(JsonValue.of(true))
        );
        assertEquals(nested1, nested2);
        assertEquals(nested1.hashCode(), nested2.hashCode());
    }

    private static void testCastingThrows() {
        assertThrows(ClassCastException.class, () -> JsonValue.ofNull().asObject());
        assertThrows(ClassCastException.class, () -> JsonValue.of(1).asBoolean());
        assertThrows(ClassCastException.class, () -> JsonValue.of("hi").asArray());
        assertThrows(ClassCastException.class, () -> JsonValue.of(true).asInt());
    }

    private static void testOfStringFactory() {
        JsonString s = JsonValue.ofString("hello");
        assertEquals("hello", s.value());
        assertEquals(JsonType.STRING, s.type());
        assertThrows(NullPointerException.class, () -> JsonValue.ofString(null));
    }
}
