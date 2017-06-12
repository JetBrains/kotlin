// EXPECTED_REACHABLE_NODES: 489
// MODULE: 1
// FILE: lib1.kt

fun foo() = "OK"

// MODULE: 2(1)
// FILE: lib2.kt

inline fun bar() = foo()

// MODULE: main(2)
// FILE: main.kt

fun box() = bar()