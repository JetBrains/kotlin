// MODULE: lib
// FILE: lib.kt

enum class Color {
    RED, GREEN, BLUE, CYAN, MAGENTA, YELLOW
}

fun determineColor(code: Int): Color = when (code) {
    0 -> Color.BLUE
    1 -> Color.MAGENTA
    else -> Color.CYAN
}

// MODULE: main(lib)
// FILE: main.kt

import kotlin.test.*

fun box(): String {
    assertEquals(0, Color.RED.ordinal)
    assertEquals(1, Color.GREEN.ordinal)
    assertEquals(2, Color.BLUE.ordinal)
    assertEquals(Color.BLUE, determineColor(0))
    assertEquals(Color.MAGENTA, determineColor(1))
    assertEquals(Color.CYAN, determineColor(2))

    return "OK"
}