package foo

fun box(): String {
    assertEquals("\u1234", js("'\\u1234'"))
    assertEquals("\u00FA", js("'\\xFA'"))
    assertEquals("\b\u000C\n\r\t\u000B\\", js("'\\b\\f\\n\\r\\t\\v\\\\'"))
    assertEquals("ab", js("'a\\\nb'"))
    assertEquals("ab\u0000\u0001\u000A\u0053\u00FFc", js("'ab\\0\\01\\012\\123\\377c'"))
    assertEquals(" 1", js("'\\401'"))
    assertEquals("\u0000", js("'\\0'"))
    assertEquals("\u0000", js(""" "\u{0}" """))
    assertEquals("A", js(""" "\u{41}" """))
    assertEquals("\u03A9", js(""" "\u{3A9}" """))
    assertEquals("\u2764", js(""" "\u{2764}" """))
    assertEquals("\uD83D\uDCA9", js(""" "\u{1F4A9}" """))
    assertEquals("\uDBC0\uDC00", js(""" "\u{100000}" """))
    assertEquals("a", js("'\\a'"))
    assertEquals("\"", js(""" '"' """))
    assertEquals("'", js(""" "'" """))
    assertEquals("`", js("'`'"))
    return "OK"
}