// WITH_STDLIB
// TARGET_BACKEND: JS_IR
// FILE: main.js
Math.trunc = undefined;

// FILE: main.kt
import kotlin.math.truncate

fun box(): String {
    val result = truncate(1.188)

    assertEquals(result, 1)
    assertEquals(js("Math.trunc.called"), js("undefined"))

    return "OK"
}
