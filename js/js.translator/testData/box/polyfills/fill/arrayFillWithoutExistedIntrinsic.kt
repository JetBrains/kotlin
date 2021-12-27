// WITH_STDLIB
// IGNORE_BACKEND: JS
// FILE: main.js
var isLegacyBackend =
    typeof Kotlin != "undefined" && typeof Kotlin.kotlin != "undefined"

if (!isLegacyBackend) {
    Array.prototype.fill = undefined
}
// FILE: main.kt
fun box(): String {
    val int = IntArray(4).apply { fill(42) }

    assertEquals(int.joinToString(", "), "42, 42, 42, 42")
    assertEquals(js("Int32Array.prototype.fill.called"), js("undefined"))

    return "OK"
}
