// WITH_STDLIB
// TARGET_BACKEND: JS_IR
// FILE: main.js
Math.sign = undefined;

// FILE: main.kt
import kotlin.math.sign

fun box(): String {
    val result = 44.0.sign

    assertEquals(result, 1)
    assertEquals(js("Math.sign.called"), js("undefined"))

    return "OK"
}
