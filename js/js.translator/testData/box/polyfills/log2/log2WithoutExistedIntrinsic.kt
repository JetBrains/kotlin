// WITH_STDLIB
// IGNORE_BACKEND: JS
// FILE: main.js
this.Math = withoutProperties(Math, ["log2"])

// FILE: main.kt
import kotlin.math.log2

fun box(): String {
    assertEquals(log2(1.0), 0)
    assertEquals(log2(2.0), 1)
    assertEquals(log2(4.0), 2)
    assertEquals(js("Math.log2.called"), js("undefined"))

    return "OK"
}
