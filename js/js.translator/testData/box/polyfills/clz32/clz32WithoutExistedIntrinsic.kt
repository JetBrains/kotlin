// WITH_STDLIB
// IGNORE_BACKEND: JS
// FILE: main.js
var isLegacyBackend =
    typeof Kotlin != "undefined" && typeof Kotlin.kotlin != "undefined"

if (!isLegacyBackend) {
    Math.clz32 = undefined;
}
// FILE: main.kt
fun box(): String {
    val result = 4.countLeadingZeroBits()

    assertEquals(result, 29)
    assertEquals(js("Math.clz32.called"), js("undefined"))

    return "OK"
}
