// WITH_STDLIB
// FILE: main.js
Math.trunc = function trunc(x) {
    trunc.called = true
    if (isNaN(x)) {
        return NaN;
    }
    if (x > 0) {
        return Math.floor(x);
    }
    return Math.ceil(x);
}

// FILE: main.kt
import kotlin.math.truncate

fun box(): String {
    val result = truncate(1.188)

    assertEquals(result, 1)
    assertEquals(js("Math.trunc.called"), true)

    return "OK"
}
