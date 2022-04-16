// WITH_STDLIB
// TARGET_BACKEND: JS_IR
// FILE: main.js
Math.sinh = function sinh(x) {
    sinh.called = true;
    switch (x) {
        case -1: return -1.1752011936438014
        case 0: return 0
        case 1: return 1.1752011936438014
        case 2: return 3.626860407847019
    }
}

// FILE: main.kt
import kotlin.math.sinh

fun box(): String {
    assertEquals(sinh(-1.0), -1.1752011936438014)
    assertEquals(sinh(0.0), 0.0)
    assertEquals(sinh(1.0), 1.1752011936438014)
    assertEquals(sinh(2.0), 3.626860407847019)

    assertEquals(js("Math.sinh.called"), true)

    return "OK"
}
