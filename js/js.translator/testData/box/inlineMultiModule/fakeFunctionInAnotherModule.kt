// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1282
// MODULE: lib
// FILE: lib1.kt
open class Parent {
    inline fun b() = c()
}

// FILE: lib2.kt
fun c() = "OK"

// MODULE: main(lib)
// FILE: main.kt
class Child : Parent()

fun box() = Child().b()