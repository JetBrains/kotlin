// IGNORE_BACKEND: JS_IR_ES6
// EXPECTED_REACHABLE_NODES: 1282
// MODULE: lib
// FILE: lib.kt
internal fun bar() = "OK"

internal inline fun foo() = bar()

// MODULE: main(lib)(lib)
// FILE: main.kt
fun box(): String = foo()