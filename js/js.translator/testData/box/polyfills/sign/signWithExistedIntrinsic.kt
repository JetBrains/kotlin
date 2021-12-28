// WITH_STDLIB
// FILE: main.js
this.Math = withMocks(Math, {
    sign(x) {
        return x > 0 ? 1 : -1;
    }
})

// FILE: main.kt
import kotlin.math.sign

fun box(): String {
    val result = 44.0.sign

    assertEquals(result, 1)
    assertEquals(js("Math.sign.called"), true)

    return "OK"
}
