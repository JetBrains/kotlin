// WITH_STDLIB

// FILE: a.kt

object Kt {
    fun sum(a: Int, b: Int): Int = a + b
    fun sumRest(vararg args: Int): Int = args.sum()
    fun greetingRest(greeting: String, vararg args: String): String = "$greeting, ${args.joinToString(", ")}"
}

object Js {
    val sum = js("function (a, b) { return a + b; }")
    val sumRest = js("function (...args) { return args.reduce((a, b) => a + b); }")
    val namedSumRest = js("function sum(...args) { return args.reduce((a, b) => a + b); }")
    val greetingRest = js("function (greeting, ...args) { return greeting + ', ' + args.join(', '); }")
}

// FILE: b.kt
// RECOMPILE

fun box(): String {
    assertEquals(Kt.sum(1, 2), Js.sum(1, 2))
    assertEquals(Kt.sumRest(1, 2, 3), Js.sumRest(1, 2, 3))
    assertEquals(Kt.sumRest(1, 2, 3), Js.namedSumRest(1, 2, 3))
    assertEquals(
        Kt.greetingRest("Hello", "K/JS", "Kotlin Multiplatform", "Compose Multiplatform"),
        Js.greetingRest("Hello", "K/JS", "Kotlin Multiplatform", "Compose Multiplatform")
    )

    return "OK"
}