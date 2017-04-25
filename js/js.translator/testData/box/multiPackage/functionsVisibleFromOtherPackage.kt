// EXPECTED_REACHABLE_NODES: 488
// FILE: a.kt
package bar

fun f() = 3;


// FILE: b.kt
package foo

import bar.*

fun box() = if (f() == 3) "OK" else "fail"