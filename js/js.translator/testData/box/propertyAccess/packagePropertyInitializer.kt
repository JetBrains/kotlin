// EXPECTED_REACHABLE_NODES: 991
package foo

val b = 3

fun box() = if (b == 3) "OK" else "fail: $b"