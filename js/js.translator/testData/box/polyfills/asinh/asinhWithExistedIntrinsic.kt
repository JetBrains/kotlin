// WITH_STDLIB
// FILE: main.js
this.Math = withMocks(Math, {
    asinh(x) {
        switch (x) {
            case -1: return -0.8813735870195429
            case 0: return 0.0
            case 1: return 0.8813735870195429
            case 2: return 1.4436354751788103
        }
    }
})

// FILE: main.kt
import kotlin.math.asinh

fun box(): String {
    assertEquals(asinh(-1.0), -0.8813735870195429)
    assertEquals(asinh(0.0), 0.0)
    assertEquals(asinh(1.0), 0.8813735870195429)
    assertEquals(asinh(2.0), 1.4436354751788103)

    assertEquals(js("Math.asinh.called"), true)

    return "OK"
}
