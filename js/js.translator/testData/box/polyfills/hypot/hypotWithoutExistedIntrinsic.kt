// WITH_STDLIB
// IGNORE_BACKEND: JS
// FILE: main.js
if (!isLegacyBackend()) {
    Math.hypot = undefined;
}

// FILE: main.kt
import kotlin.math.hypot

fun box(): String {
    assertEquals(hypot(3.0, 4.0), 5.0)
    assertEquals(hypot(5.0, 12.0), 13.0)
    assertEquals(hypot(-5.0, 0.0), 5.0)
    assertEquals(js("Math.hypot.called"), js("undefined"))

    return "OK"
}
