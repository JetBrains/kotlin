// EXPECTED_REACHABLE_NODES: 491
// MODULE: lib
// FILE: lib.kt
class A {
    fun instanceof() = "OK"
}

inline fun foo() = A().instanceof()

// MODULE: main(lib)
// FILE: main.kt

fun box() = foo()