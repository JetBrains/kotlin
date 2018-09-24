// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1282
// MODULE: lib
// FILE: lib.kt
fun foo() = "OK"

// MODULE: main(lib)
// FILE: main1.kt
inline fun bar() = foo()

// FILE: main2.kt
// RECOMPILE
fun box() = bar()
