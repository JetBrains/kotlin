// WITH_STDLIB
// IGNORE_BACKEND: JS
// FILE: main.js
var isLegacyBackend =
    typeof Kotlin != "undefined" && typeof Kotlin.kotlin != "undefined"

if (!isLegacyBackend) {
    Math.atanh = function atanh(x) {
        atanh.called = true
        switch (x) {
            case -1: return -Infinity
            case 0: return 0.0
            case 0.5: return 0.5493061443340548
            case 1: return Infinity
        }
    }
}
// FILE: main.kt
import kotlin.math.atanh

fun box(): String {
    assertEquals(atanh(-1.0), Double.NEGATIVE_INFINITY)
    assertEquals(atanh(0.0), 0.0)
    assertEquals(atanh(0.5), 0.5493061443340548)
    assertEquals(atanh(1.0), Double.POSITIVE_INFINITY)

    assertEquals(js("Math.atanh.called"), true)

    return "OK"
}
