// WITH_STDLIB
// TARGET_BACKEND: JS_IR
// FILE: main.js
Math.sign = function sign(x) {
    sign.called = true;
    return x > 0 ? 1 : -1;
}

// FILE: main.kt
import kotlin.math.sign

fun box(): String {
    val result = 44.0.sign

    assertEquals(result, 1)
    assertEquals(js("Math.sign.called"), true)

    return "OK"
}
