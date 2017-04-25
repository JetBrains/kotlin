// EXPECTED_REACHABLE_NODES: 492
package foo

fun f() {}
fun selector() = true

val y = if (selector()) 23 else throw Exception()
val x = if (selector()) { f(); y + 1 } else 999
val z = if (selector()) { f(); x + 1 } else 999

fun box(): String {
    if (x != 24) return "fail1: $x"
    if (z != 25) return "fail2: $z"
    return "OK"
}