// IGNORE_BACKEND: JS
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

package foo

class class_with_invalid_chars {
    companion object {
        fun foo(): Int = 23
    }
}

class `class@with$invalid chars` {
    companion object {
        fun foo(): Int = 42
    }
}

fun box(): String {
    assertEquals(23, class_with_invalid_chars.foo())
    assertEquals(42, `class@with$invalid chars`.foo())

    return "OK"
}