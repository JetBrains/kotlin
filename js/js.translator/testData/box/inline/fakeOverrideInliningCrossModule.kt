// EXPECTED_REACHABLE_NODES: 1300
// MODULE: libA
// FILE: libA.kt
inline fun foo() = (object : II {}).ok()

interface I {
    fun ok() = "OK"
}

interface II: I

// MODULE: libB(libA)
// FILE: libB.kt

inline fun bar() = foo()

// MODULE: main(libB)
// FILE: main.kt
fun box() = bar()


