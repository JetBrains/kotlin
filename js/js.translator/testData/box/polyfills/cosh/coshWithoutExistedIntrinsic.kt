// WITH_STDLIB
// IGNORE_BACKEND: JS
// FILE: main.js
Math.cosh = undefined

// FILE: main.kt
import kotlin.math.cosh

fun box(): String {
    assertEquals(cosh(-1.0), 1.5430806348152437)
    assertEquals(cosh(0.0), 1.0)
    assertEquals(cosh(1.0), 1.5430806348152437)
    assertEquals(cosh(2.0), 3.7621956910836314)

    assertEquals(js("Math.cosh.called"), js("undefined"))

    return "OK"
}
