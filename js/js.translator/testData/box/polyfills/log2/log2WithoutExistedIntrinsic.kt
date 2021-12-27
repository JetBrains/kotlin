// WITH_STDLIB
// IGNORE_BACKEND: JS
// FILE: main.js
var isLegacyBackend =
    typeof Kotlin != "undefined" && typeof Kotlin.kotlin != "undefined"

if (!isLegacyBackend) {
    Math.log2 = undefined
}
// FILE: main.kt
import kotlin.math.log2

fun box(): String {
    assertEquals(log2(1.0), 0)
    assertEquals(log2(2.0), 1)
    assertEquals(log2(4.0), 2)
    assertEquals(js("Math.log2.called"), js("undefined"))

    return "OK"
}
