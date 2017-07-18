// EXPECTED_REACHABLE_NODES: 991
// FILE: a.kt
package a.foo

fun box() = b.bar.f()


// FILE: b.kt
package b.bar

fun f() = "OK"