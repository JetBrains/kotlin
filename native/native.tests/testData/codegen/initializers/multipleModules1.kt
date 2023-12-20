// MODULE: lib
// FILE: lib.kt

val a = 10
val b = a * 6
val c = a * b * 2

// MODULE: main(lib)
// FILE: main.kt

import kotlin.test.*

fun box(): String {
    assertEquals(1200, c)
    return "OK"
}