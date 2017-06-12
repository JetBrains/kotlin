// EXPECTED_REACHABLE_NODES: 488
package foo

fun test(x: Int, y: Int) = y - x

fun box(): String {
    if (test(1, 2) != 1) {
        return "fail1"
    }
    if (test(x = 1, y = 2) != 1) {
        return "fail2"
    }
    if (test(y = 2, x = 1) != 1) {
        return "fail3"
    }
    return "OK"
}