// EXPECTED_REACHABLE_NODES: 1116
// MODULE: lib
// FILE: lib.kt
class A {
    fun instanceof() = "OK"
}

inline fun foo() = A().instanceof()

// MODULE: main(lib)
// FILE: main.kt

fun box() = foo()