package com.keerthan.httpserver.util;

import java.util.Map;
import java.util.StringJoiner;

/** Minimal JSON writer, enough for this server's responses (no external libraries). */
public final class Json {

    private Json() {
    }

    public static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    /** Builds an object from alternating key/value arguments: object("a", 1, "b", "x"). */
    public static String object(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Expected key/value pairs");
        }
        StringJoiner joiner = new StringJoiner(",", "{", "}");
        for (int i = 0; i < keyValues.length; i += 2) {
            joiner.add(quote(String.valueOf(keyValues[i])) + ":" + value(keyValues[i + 1]));
        }
        return joiner.toString();
    }

    public static String value(Object v) {
        if (v == null) {
            return "null";
        }
        if (v instanceof Number || v instanceof Boolean) {
            return v.toString();
        }
        if (v instanceof Map<?, ?> map) {
            StringJoiner joiner = new StringJoiner(",", "{", "}");
            map.forEach((k, val) -> joiner.add(quote(String.valueOf(k)) + ":" + value(val)));
            return joiner.toString();
        }
        return quote(v.toString());
    }
}
