// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1282
// FILE: a.kt
package foo

val a = 2

fun box() = if ((a + bar.a) == 5) "OK" else "fail"


// FILE: b.kt
package bar

val a = 3
