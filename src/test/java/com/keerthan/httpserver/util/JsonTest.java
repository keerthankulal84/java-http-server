package com.keerthan.httpserver.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonTest {

    @Test
    void escapesSpecialCharacters() {
        assertEquals("\"a\\\"b\\\\c\\n\"", Json.quote("a\"b\\c\n"));
    }

    @Test
    void buildsObjectWithMixedTypes() {
        assertEquals("{\"name\":\"x\",\"n\":3,\"ok\":true}", Json.object("name", "x", "n", 3, "ok", true));
    }
}
