// EXPECTED_REACHABLE_NODES: 490
// MODULE: lib1
// FILE: lib1.kt
inline fun foo() = bar()

fun bar() = "OK"

// MODULE: lib2(lib1)
// FILE: lib2.kt

inline fun baz() = foo()

// MODULE: main(lib2)
// FILE: main.kt

fun box() = baz()