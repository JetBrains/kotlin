// EXPECTED_REACHABLE_NODES: 488
package foo

fun box(): String {
    if (f(0) != 201) {
        return "fail1"
    }
    if (f(1) != 104) {
        return "fail2"
    }
    if (f(-2) != -100) {
        return "fail3"
    }
    return "OK"
}

fun f(i: Int): Int {
    var j = i
    return ++j + if (j != 1) {
        (if (j > 0) 100 else return -100) + 2
    }
    else 200
}