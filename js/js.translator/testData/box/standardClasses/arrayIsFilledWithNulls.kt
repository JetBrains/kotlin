// EXPECTED_REACHABLE_NODES: 994
package foo

val a = arrayOfNulls<Int>(3)

fun box() = if (a[0] == null && a[1] == null && a[2] == null) "OK" else "fail"