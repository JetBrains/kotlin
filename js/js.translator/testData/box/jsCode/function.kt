// FILE: a.kt

object Kt {
    fun sum(a: Int, b: Int): Int = a + b
}
object Js {
    val sum = js("function (a, b) { return a + b; }")
}

// FILE: b.kt
// RECOMPILE

fun box(): String {
    assertEquals(Kt.sum(1, 2), Js.sum(1, 2))

    return "OK"
}