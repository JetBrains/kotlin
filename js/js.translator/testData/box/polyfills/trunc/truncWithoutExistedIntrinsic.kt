// WITH_STDLIB
// IGNORE_BACKEND: JS
// FILE: main.js
var isLegacyBackend =
    typeof Kotlin != "undefined" && typeof Kotlin.kotlin != "undefined"

if (!isLegacyBackend) {
    Math.trunc = undefined
}
// FILE: main.kt
import kotlin.math.truncate

fun box(): String {
    val result = truncate(1.188)

    assertEquals(result, 1)
    assertEquals(js("Math.trunc.called"), js("undefined"))

    return "OK"
}
