// WITH_STDLIB
// TARGET_BACKEND: JS_IR
// FILE: main.js
Math.log10 = undefined;

// FILE: main.kt
import kotlin.math.log10

fun box(): String {
    assertEquals(log10(1.0), 0)
    assertEquals(log10(10.0), 1)
    assertEquals(log10(100.0), 2)
    assertEquals(js("Math.log10.called"), js("undefined"))

    return "OK"
}
