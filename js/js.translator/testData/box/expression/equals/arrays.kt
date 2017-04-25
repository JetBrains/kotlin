// EXPECTED_REACHABLE_NODES: 487
package foo

fun box(): String {
    val a = arrayOf(1, 2, 3)
    val b = arrayOf(1, 2, 3)
    val c = a

    if (a == b) return "fail1"
    if (a != c) return "fail2"
    if (c == b) return "fail3"

    return "OK"
}