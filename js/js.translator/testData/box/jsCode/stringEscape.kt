package foo

fun box(): String {
    assertEquals("\u1234", js("'\\u1234'"))
    assertEquals("\u00FA", js("'\\xFA'"))
    assertEquals("\b\u000C\n\r\t\u000B\\", js("'\\b\\f\\n\\r\\t\\v\\\\'"))
    assertEquals("ab", js("'a\\\nb'"))
    assertEquals("ab\u0000\u0001\u000A\u0053\u00FFc", js("'ab\\0\\01\\012\\123\\377c'"))
    assertEquals(" 1", js("'\\401'"))
    assertEquals("\u0000", js("'\\0'"))
    assertEquals("a", js("'\\a'"))
    assertEquals("\"", js(""" '"' """))
    assertEquals("'", js(""" "'" """))
    assertEquals("`", js("'`'"))
    return "OK"
}