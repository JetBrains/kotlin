// EXPECTED_REACHABLE_NODES: 488
// PROPERTY_WRITE_COUNT: name=foo_61zpoe$ count=1
// FILE: a.kt
fun foo(x: String): String = x

inline fun o() = foo("O")

// FILE: b.kt
inline fun k() = foo("K")

// FILE: c.kt
// RECOMPILE
fun box() = o() + k()