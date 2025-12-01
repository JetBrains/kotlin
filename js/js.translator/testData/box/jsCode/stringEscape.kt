package foo
fun box(): String {
    assertEquals(js("'\\u1234'"), "\u1234")
    assertEquals(js("'\\xFA'"), "\u00FA")
    assertEquals(js("'\\b\\f\\n\\r\\t\\v\\\\'"), "\b\u000C\n\r\t\u000B\\")
    assertEquals(js("'ab\\0\\01\\012\\123\\377c'"), "ab\u0000\u0001\u000A\u0053\u00FFc")
    assertEquals(js("'\\401'"), " 1")
    assertEquals(js("'\\0'"), "\u0000")
    assertEquals(js("'\\a'"), "a")
    assertEquals(js(""" '"' """), "\"")
    assertEquals(js(""" "'" """), "'")
    assertEquals(js("'`'"), "`")
    return "OK"
}
