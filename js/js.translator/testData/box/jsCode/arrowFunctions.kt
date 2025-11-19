// FILE: a.kt

val one = js("() => 1")
val sum = js("(a, b) => a + b")
val id = js("a => a")
val block = js("(a) => { return 'Hello, ' + a; }")

// FILE: b.kt
// RECOMPILE

fun box(): String {
    assertEquals(1, one())
    assertEquals(3, sum(1, 2))
    assertEquals(4, id(4))
    assertEquals("Hello, K/JS", block("K/JS"))

    return "OK"
}