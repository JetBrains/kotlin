// EXPECTED_REACHABLE_NODES: 493
package foo

class Test(a: Int, b: Int) {
    val c = a
    val d = b
}

fun box(): String {
    val test = Test(1 + 6 * 3, 10 % 2)
    if (test.c != 19) return "fail1"
    if (test.d != 0) return "fail2"

    return "OK"
}