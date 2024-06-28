// IGNORE_BACKEND: JS
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

package foo

@JsExport()
class A {
    val `#invalid@char value`: Int = 41
    val __invalid_char_value: Int = 23

    var `--invalud char@var`: String = "A: before"
}

class B {
    val `#invalid@char value`: Int = 42
    val __invalid_char_value: Int = 24

    var `--invalud char@var`: String = "B: before"
}

fun box(): String {
    val a = A()
    val b = B()

    assertEquals(23, a.__invalid_char_value)
    assertEquals(24, b.__invalid_char_value)

    assertEquals(41, a.`#invalid@char value`)
    assertEquals(42, b.`#invalid@char value`)

    assertEquals("A: before", a.`--invalud char@var`)
    assertEquals("B: before", b.`--invalud char@var`)

    a.`--invalud char@var` = "A: after"
    b.`--invalud char@var` = "B: after"

    assertEquals("A: after", a.`--invalud char@var`)
    assertEquals("B: after", b.`--invalud char@var`)

    assertEquals(41, js("a['#invalid@char value']"))
    assertEquals(js("undefined"), js("b['#invalid@char value']"))

    assertEquals("A: after", js("a['--invalud char@var']"))
    assertEquals(js("undefined"), js("b['--invalud char@var']"))

    return "OK"
}