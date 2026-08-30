package com.jvalue;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * JSON serializer for the {@link JsonValue} hierarchy.
 */
final class JsonSerializer {

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
    private static final int INDENT_WIDTH = 2;

    private JsonSerializer() {
        // Static utility class.
    }

    static String serialize(JsonValue value) {
        StringBuilder out = new StringBuilder();
        try {
            write(value, out);
        } catch (IOException e) {
            throw new AssertionError("StringBuilder should not throw IOException", e);
        }
        return out.toString();
    }

    static void write(JsonValue value, Appendable out) throws IOException {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(out, "out");
        writeValue(value, out);
    }

    static String serializePretty(JsonValue value) {
        StringBuilder out = new StringBuilder();
        try {
            writePretty(value, out);
        } catch (IOException e) {
            throw new AssertionError("StringBuilder should not throw IOException", e);
        }
        return out.toString();
    }

    static void writePretty(JsonValue value, Appendable out) throws IOException {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(out, "out");
        writePrettyValue(value, out, 0);
    }

    private static void writeValue(JsonValue value, Appendable out) throws IOException {
        switch (value) {
            case JsonNull ignored -> out.append("null");
            case JsonBoolean bool -> out.append(bool.value() ? "true" : "false");
            case JsonNumber number -> writeNumber(number, out);
            case JsonString string -> writeString(string.value(), out);
            case JsonArray array -> writeArray(array, out);
            case JsonObject object -> writeObject(object, out);
        }
    }

    private static void writeArray(JsonArray array, Appendable out) throws IOException {
        out.append('[');
        boolean first = true;
        for (JsonValue element : array) {
            if (!first) {
                out.append(',');
            }
            writeValue(element, out);
            first = false;
        }
        out.append(']');
    }

    private static void writeObject(JsonObject object, Appendable out) throws IOException {
        out.append('{');
        boolean first = true;
        for (Map.Entry<String, JsonValue> entry : object) {
            if (!first) {
                out.append(',');
            }
            writeString(entry.getKey(), out);
            out.append(':');
            writeValue(entry.getValue(), out);
            first = false;
        }
        out.append('}');
    }

    private static void writePrettyValue(JsonValue value, Appendable out, int depth) throws IOException {
        switch (value) {
            case JsonNull ignored -> out.append("null");
            case JsonBoolean bool -> out.append(bool.value() ? "true" : "false");
            case JsonNumber number -> writeNumber(number, out);
            case JsonString string -> writeString(string.value(), out);
            case JsonArray array -> writePrettyArray(array, out, depth);
            case JsonObject object -> writePrettyObject(object, out, depth);
        }
    }

    private static void writePrettyArray(JsonArray array, Appendable out, int depth) throws IOException {
        if (array.isEmpty()) {
            out.append("[]");
            return;
        }

        out.append('[');
        out.append('\n');
        boolean first = true;
        for (JsonValue element : array) {
            if (!first) {
                out.append(',');
                out.append('\n');
            }
            writeIndent(out, depth + 1);
            writePrettyValue(element, out, depth + 1);
            first = false;
        }
        out.append('\n');
        writeIndent(out, depth);
        out.append(']');
    }

    private static void writePrettyObject(JsonObject object, Appendable out, int depth) throws IOException {
        if (object.isEmpty()) {
            out.append("{}");
            return;
        }

        out.append('{');
        out.append('\n');
        boolean first = true;
        for (Map.Entry<String, JsonValue> entry : object) {
            if (!first) {
                out.append(',');
                out.append('\n');
            }
            writeIndent(out, depth + 1);
            writeString(entry.getKey(), out);
            out.append(": ");
            writePrettyValue(entry.getValue(), out, depth + 1);
            first = false;
        }
        out.append('\n');
        writeIndent(out, depth);
        out.append('}');
    }

    private static void writeIndent(Appendable out, int depth) throws IOException {
        for (int i = 0; i < depth * INDENT_WIDTH; i++) {
            out.append(' ');
        }
    }

    private static void writeNumber(JsonNumber number, Appendable out) throws IOException {
        String raw = number.raw();
        if (!isValidJsonNumber(raw)) {
            throw new IllegalArgumentException("Invalid JSON number: " + raw);
        }
        out.append(raw);
    }

    private static boolean isValidJsonNumber(String raw) {
        int length = raw.length();
        int index = 0;

        if (raw.charAt(index) == '-') {
            index++;
            if (index == length) {
                return false;
            }
        }

        char first = raw.charAt(index);
        if (first == '0') {
            index++;
            if (index < length && isDigit(raw.charAt(index))) {
                return false;
            }
        } else if (first >= '1' && first <= '9') {
            index++;
            while (index < length && isDigit(raw.charAt(index))) {
                index++;
            }
        } else {
            return false;
        }

        if (index < length && raw.charAt(index) == '.') {
            index++;
            if (index == length || !isDigit(raw.charAt(index))) {
                return false;
            }
            while (index < length && isDigit(raw.charAt(index))) {
                index++;
            }
        }

        if (index < length && (raw.charAt(index) == 'e' || raw.charAt(index) == 'E')) {
            index++;
            if (index < length && (raw.charAt(index) == '+' || raw.charAt(index) == '-')) {
                index++;
            }
            if (index == length || !isDigit(raw.charAt(index))) {
                return false;
            }
            while (index < length && isDigit(raw.charAt(index))) {
                index++;
            }
        }

        return index == length;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static void writeString(String value, Appendable out) throws IOException {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c <= '\u001F') {
                        writeUnicodeEscape(c, out);
                    } else if (Character.isHighSurrogate(c)) {
                        writeSurrogatePair(value, i, out);
                        i++;
                    } else if (Character.isLowSurrogate(c)) {
                        throw new IllegalArgumentException(
                                "Lone low surrogate U+" + hex4(c) + " cannot be serialized");
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }

    private static void writeSurrogatePair(String value, int highIndex, Appendable out) throws IOException {
        char high = value.charAt(highIndex);
        if (highIndex + 1 >= value.length()) {
            throw new IllegalArgumentException("Lone high surrogate U+" + hex4(high) + " cannot be serialized");
        }
        char low = value.charAt(highIndex + 1);
        if (!Character.isLowSurrogate(low)) {
            throw new IllegalArgumentException("Lone high surrogate U+" + hex4(high) + " cannot be serialized");
        }
        out.append(high);
        out.append(low);
    }

    private static void writeUnicodeEscape(char c, Appendable out) throws IOException {
        out.append("\\u");
        out.append(HEX[(c >>> 12) & 0xF]);
        out.append(HEX[(c >>> 8) & 0xF]);
        out.append(HEX[(c >>> 4) & 0xF]);
        out.append(HEX[c & 0xF]);
    }

    private static String hex4(char c) {
        return new String(new char[] {
                HEX[(c >>> 12) & 0xF],
                HEX[(c >>> 8) & 0xF],
                HEX[(c >>> 4) & 0xF],
                HEX[c & 0xF]
        });
    }
}
