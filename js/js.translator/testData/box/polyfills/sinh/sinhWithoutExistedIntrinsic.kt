// WITH_STDLIB
// IGNORE_BACKEND: JS
// FILE: main.js
this.Math = withoutProperties(Math, ["sinh"])

// FILE: main.kt
import kotlin.math.sinh

fun box(): String {
    assertEquals(sinh(-1.0), -1.1752011936438014)
    assertEquals(sinh(0.0), 0.0)
    assertEquals(sinh(1.0), 1.1752011936438014)
    assertEquals(sinh(2.0), 3.626860407847019)

    assertEquals(js("Math.sinh.called"), js("undefined"))

    return "OK"
}
