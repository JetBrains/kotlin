// EXPECTED_REACHABLE_NODES: 488
// FILE: a.kt
package foo

fun f() = 3;


// FILE: b.kt
package foo

fun box() = if (f() == 3) "OK" else "fail"