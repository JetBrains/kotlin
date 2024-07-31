// EXPECTED_REACHABLE_NODES: 1281
// FILE: a.kt
fun foo(x: String): String = x

inline fun o() = foo("O")

// FILE: b.kt
inline fun k() = foo("K")

// FILE: c.kt
// RECOMPILE
fun box() = o() + k()