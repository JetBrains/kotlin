// EXPECTED_REACHABLE_NODES: 524
package foo

object State {
    val c = 2
    val b = 1
}

fun box(): String {
    if (State.c != 2) return "fail1: ${State.c}"
    if (State.b != 1) return "fail2: ${State.b}"
    return "OK"
}