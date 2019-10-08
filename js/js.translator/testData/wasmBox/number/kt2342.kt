// EXPECTED_REACHABLE_NODES: 1281
package foo

fun test(a: Int, b: Int, expected: Int): Int {
    val result = a / b
    if (expected == result) return 100
    return 25
}

fun box(): String {
    var r = test(10, 3, 3)
    if (r != 100) return "Fail1"

    r = test(49, 6, 8)
    if (r != 100) return "Fail 2"

    if (2133 / 3 / 7 / (91 / 5) != 5) return "2133 / 3 / 7 / (91 / 5) != 5"

    return "OK"
}