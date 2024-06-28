// MODULE: lib
// FILE: lib.kt

val a = mutableListOf<String>()
val b = 1.also { a += "OK" }
val c = a.single()

// MODULE: main(lib)
// FILE: main.kt

fun box() = c