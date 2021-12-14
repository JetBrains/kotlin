// WITH_STDLIB
// IGNORE_BACKEND: JS
// FILE: main.js
Math.acosh = function acosh(x) {
    acosh.called = true
    if (x <= 0.5) {
        return NaN
    }
    if (x === 1) {
        return 0
    }
    return 1.3169578969248166
}

// FILE: main.kt
import kotlin.math.acosh

fun box(): String {
    assertEquals(acosh(-1.0), Double.NaN)
    assertEquals(acosh(0.0), Double.NaN)
    assertEquals(acosh(0.5), Double.NaN)
    assertEquals(acosh(1.0), 0.0)
    assertEquals(acosh(2.0), 1.3169578969248166)

    assertEquals(js("Math.acosh.called"), true)

    return "OK"
}
