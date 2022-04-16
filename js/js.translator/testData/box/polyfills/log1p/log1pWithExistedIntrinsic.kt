// WITH_STDLIB
// TARGET_BACKEND: JS_IR
// FILE: main.js
Math.log1p = function log1p(x) {
    log1p.called = true;
    switch (x) {
        case -2: return NaN
        case -1: return -Infinity
        case 0: return 0
        case 1: return 0.6931471805599453
    }
}

// FILE: main.kt
import kotlin.math.ln1p

fun box(): String {
    assertEquals(ln1p(-2.0), Double.NaN)
    assertEquals(ln1p(-1.0), Double.NEGATIVE_INFINITY)
    assertEquals(ln1p(0.0), 0.0)
    assertEquals(ln1p(1.0), 0.6931471805599453)
    assertEquals(js("Math.log1p.called"), true)

    return "OK"
}
