// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1282
// FILE: a.kt
package foo.bar

fun o() = "O"

// FILE: b.kt
package foo.bar

fun k() = "K"

// FILE: c.kt
// RECOMPILE
package foo.bar

fun box() = o() + k()