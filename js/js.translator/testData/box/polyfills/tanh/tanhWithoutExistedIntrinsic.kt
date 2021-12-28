// WITH_STDLIB
// IGNORE_BACKEND: JS
// FILE: main.js
if (!isLegacyBackend()) {
    Math.tanh = undefined;
}

// FILE: main.kt
import kotlin.math.tanh

fun box(): String {
    assertEquals(tanh(-1.0), -0.7615941559557649)
    assertEquals(tanh(0.0), 0.0)
    assertEquals(tanh(1.0), 0.7615941559557649)
    assertEquals(tanh(Double.POSITIVE_INFINITY), 1.0)
    assertEquals(js("Math.tanh.called"), js("undefined"))

    return "OK"
}
