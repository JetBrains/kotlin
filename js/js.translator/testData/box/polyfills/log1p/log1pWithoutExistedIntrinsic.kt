// WITH_STDLIB
// IGNORE_BACKEND: JS
// FILE: main.js
var isLegacyBackend =
    typeof Kotlin != "undefined" && typeof Kotlin.kotlin != "undefined"

if (!isLegacyBackend) {
    Math.log1p = undefined
}
// FILE: main.kt
import kotlin.math.ln1p

fun box(): String {
    assertEquals(ln1p(-2.0), Double.NaN)
    assertEquals(ln1p(-1.0), Double.NEGATIVE_INFINITY)
    assertEquals(ln1p(0.0), 0.0)
    assertEquals(ln1p(1.0), 0.6931471805599453)
    assertEquals(js("Math.log1p.called"), js("undefined"))

    return "OK"
}
