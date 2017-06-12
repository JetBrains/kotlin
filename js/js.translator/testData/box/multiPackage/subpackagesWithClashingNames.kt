// EXPECTED_REACHABLE_NODES: 488
// FILE: a.kt
package a.foo

fun box() = if (b.foo.f() == 1) "OK" else "fail"


// FILE: b.kt
package b.foo

fun f() = 1