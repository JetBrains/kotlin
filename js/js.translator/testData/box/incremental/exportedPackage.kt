// EXPECTED_REACHABLE_NODES: 1251
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