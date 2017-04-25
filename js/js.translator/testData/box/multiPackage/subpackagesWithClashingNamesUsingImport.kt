// EXPECTED_REACHABLE_NODES: 488
// FILE: a.kt
package a.foo

import b.foo.f

fun box() = if (f() == 1) "OK" else "fail"


// FILE: b.kt
package b.foo

fun f() = 1