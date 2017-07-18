// EXPECTED_REACHABLE_NODES: 998
// MODULE: lib
// FILE: lib.kt
class A {
    fun instanceof() = "OK"
}

inline fun foo() = A().instanceof()

// MODULE: main(lib)
// FILE: main.kt

fun box() = foo()