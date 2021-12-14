// WITH_STDLIB
// IGNORE_BACKEND: JS
// FILE: main.js
Math.atanh = undefined

// FILE: main.kt
import kotlin.math.atanh

fun box(): String {
    assertEquals(atanh(-1.0), Double.NEGATIVE_INFINITY)
    assertEquals(atanh(0.0), 0.0)
    assertEquals(atanh(0.5), 0.5493061443340548)
    assertEquals(atanh(1.0), Double.POSITIVE_INFINITY)

    assertEquals(js("Math.atanh.called"), js("undefined"))

    return "OK"
}
