// EXPECTED_REACHABLE_NODES: 488
package foo

fun box(): String {
    if (f(0) != -3) {
        return "fail1"
    }
    if (f(102) != 201) {
        return "fail2"
    }
    if (f(103) != 100) {
        return "fail3"
    }
    if (f(-100) != -100) {
        return "fail4"
    }
    if (f(-99) != -201) {
        return "fail5"
    }

    return "OK"
}

fun f(i: Int): Int {
    var j = i
    return --j + (if (j < -100) return -100 else --j) + (if (j > 100) return 100 else 0)
}
