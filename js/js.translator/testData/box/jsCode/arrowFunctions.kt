// FILE: a.kt

val one = js("() => 1")
val sum = js("(a, b) => a + b")
val sumRest = js("(...args) => args.reduce((a, b) => a + b)")
val id = js("a => a")
val block = js("(a) => { return 'Hello, ' + a; }")
val blockRest = js("(greeting, ...args) => { return greeting + ', ' + args.join(', '); }")

// FILE: b.kt
// RECOMPILE

fun box(): String {
    assertEquals(1, one())
    assertEquals(3, sum(1, 2))
    assertEquals(6, sumRest(1, 2, 3))
    assertEquals(4, id(4))
    assertEquals("Hello, K/JS", block("K/JS"))
    assertEquals("Hello, K/JS, Kotlin Multiplatform, Compose Multiplatform",
                 blockRest("Hello", "K/JS", "Kotlin Multiplatform", "Compose Multiplatform"))

    return "OK"
}