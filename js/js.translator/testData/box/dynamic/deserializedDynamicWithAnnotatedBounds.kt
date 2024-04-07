// MODULE: lib
// FILE: Lib.kt

fun <R> test(it: dynamic, block: (param: dynamic) -> R) = block(it)

// MODULE: main(lib)
// FILE: Main.kt

fun box(): String = test("O") { it + "K" }
