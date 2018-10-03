// EXPECTED_REACHABLE_NODES: 1281
// FILE: a.kt
package a.foo

fun box() = if (b.foo.f() == 1) "OK" else "fail"


// FILE: b.kt
package b.foo

fun f() = 1