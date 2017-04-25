// EXPECTED_REACHABLE_NODES: 487
package foo

fun box(): String {
    val a = "abc"
    val b = "abc"
    val c = "def"

    if (a != b) return "fail1"
    if (a == c) return "fail2"

    return "OK"
}