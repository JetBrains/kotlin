// WITH_STDLIB
// TARGET_BACKEND: JS_IR
// FILE: main.js
Math.log2 = undefined;

// FILE: main.kt
import kotlin.math.log2

fun box(): String {
    assertEquals(log2(1.0), 0)
    assertEquals(log2(2.0), 1)
    assertEquals(log2(4.0), 2)
    assertEquals(js("Math.log2.called"), js("undefined"))

    return "OK"
}
