// EXPECTED_REACHABLE_NODES: 1192
// IGNORE_BACKEND: JS_IR

// MODULE: lib
// FILE: lib.kt


inline fun baz() = pp()

fun pp(): String = "OK"

// MODULE: mid(lib)
// FILE: mid.kt

fun bar() {
    foo()
}

inline fun foo()= baz()

// MODULE: main(mid)
// FILE: main.kt

fun box() = foo()