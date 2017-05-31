// EXPECTED_REACHABLE_NODES: 491
// MODULE: lib1
// FILE: lib1.kt

fun foo() = "O"

fun bar() = "K"

// MODULE: lib2(lib1)
// FILE: lib2a.kt
// PROPERTY_WRITE_COUNT: name=lib1 count=1
// PROPERTY_WRITE_COUNT: name=$$imports$$ count=1

inline fun o() = foo()

// FILE: lib2b.kt

inline fun k() = bar()

// MODULE: main(lib2)
// FILE: main.kt
// RECOMPILE

fun box() = o() + k()