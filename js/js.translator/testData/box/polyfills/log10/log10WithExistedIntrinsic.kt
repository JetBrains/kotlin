// WITH_STDLIB
// FILE: main.js
this.Math = withMocks(Math, {
    log10(x) {
        return Math.log(x) * Math.LOG10E;
    }
})

// FILE: main.kt
import kotlin.math.log10

fun box(): String {
    assertEquals(log10(1.0), 0)
    assertEquals(log10(10.0), 1)
    assertEquals(log10(100.0), 2)
    assertEquals(js("Math.log10.called"), true)

    return "OK"
}
