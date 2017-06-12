// EXPECTED_REACHABLE_NODES: 488
package foo

fun box(): String {
    if (t(1) != 0) {
        return "fail1"
    }
    if (t(0) != 1) {
        return "fail2"
    }
    if (t(100) != 2) {
        return "fail3"
    }
    return "OK"
}

fun t(i: Int) = when(i) {
    0 -> 1
    1 -> 0
    else -> 2
}