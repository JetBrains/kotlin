// EXPECTED_REACHABLE_NODES: 1269
// IGNORE_BACKEND: JS_IR

// MODULE: lib
// FILE: lib.kt

class A(k: String) {
    val ok = "O" + k
}

inline fun o(k: String) = A(k).ok

// MODULE: main(lib)
// FILE: main.kt

class B {
    val ok = run { o("K") }
}

fun box() = B().ok