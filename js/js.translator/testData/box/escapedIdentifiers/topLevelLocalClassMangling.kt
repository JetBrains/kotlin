// IGNORE_BACKEND: JS
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

package foo

class class_with_invalid_chars {
    fun foo(): Int = 23
}

class `class@with$invalid chars` {
    fun foo(): Int = 42
}

fun box(): String {
    val a = class_with_invalid_chars()
    val b = `class@with$invalid chars`()

    assertEquals(true, a is class_with_invalid_chars)
    assertEquals(true, b is `class@with$invalid chars`)

    assertEquals(23, a.foo())
    assertEquals(42, b.foo())

    return "OK"
}