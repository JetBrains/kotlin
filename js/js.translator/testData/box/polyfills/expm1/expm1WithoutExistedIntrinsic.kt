// WITH_STDLIB
// TARGET_BACKEND: JS_IR
// FILE: main.js
Math.expm1 = undefined;

// FILE: main.kt
import kotlin.math.expm1

fun box(): String {
    assertEquals(expm1(-1.0), -0.6321205588285577)
    assertEquals(expm1(0.0), 0.0)
    assertEquals(expm1(1.0), 1.718281828459045)
    assertEquals(expm1(2.0), 6.38905609893065)

    assertEquals(js("Math.expm1.called"), js("undefined"))

    return "OK"
}
