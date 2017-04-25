// EXPECTED_REACHABLE_NODES: 488
package foo

fun f(a: Int = 2, b: Int = 3) = a + b

fun box(): String {
    if (f(1, 2) != 3) return "fail1"
    if (f(1, 3) != 4) return "fail2"
    if (f(3) != 6) return "fail3"
    if (f() != 5) return "fail4"

    return "OK"
}

