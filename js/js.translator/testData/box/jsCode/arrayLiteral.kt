// FILE: a.kt

val simple = js("[1, 2, 3]")

// FILE: b.kt
// RECOMPILE

fun box(): String {
    assertArrayEquals(arrayOf(1, 2, 3), simple, "Simple")

    return "OK"
}