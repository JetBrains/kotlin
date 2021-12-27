// WITH_STDLIB
// FILE: main.js
Math.cosh = function cosh(x) {
    cosh.called = true
    switch (x) {
       case -1: return 1.5430806348152437
       case 0: return 1.0
       case 1: return 1.5430806348152437
       case 2: return 3.7621956910836314
    }
}

// FILE: main.kt
import kotlin.math.cosh

fun box(): String {
    assertEquals(cosh(-1.0), 1.5430806348152437)
    assertEquals(cosh(0.0), 1.0)
    assertEquals(cosh(1.0), 1.5430806348152437)
    assertEquals(cosh(2.0), 3.7621956910836314)

    assertEquals(js("Math.cosh.called"), true)

    return "OK"
}
