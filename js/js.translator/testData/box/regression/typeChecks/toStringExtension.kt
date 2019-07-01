// EXPECTED_REACHABLE_NODES: 1281

package foo

class A

fun A?.toString() = "OK"

fun box() = (null as A?).toString()