// EXPECTED_REACHABLE_NODES: 1282
// SKIP_IR_INCREMENTAL_CHECKS
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