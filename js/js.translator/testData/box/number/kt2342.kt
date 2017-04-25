// EXPECTED_REACHABLE_NODES: 488
package foo

fun test(a: Int, b: Int, expected: Int): String {
    val result = a / b
    if (expected == result) return "OK"
    return "$a / $b = $result. Expected $expected"
}

fun box(): String {
    var r = test(10, 3, 3)
    if (r != "OK") return r

    r = test(49, 6, 8)
    if (r != "OK") return r

    if (2133 / 3 / 7 / (91 / 5) != 5) return "2133 / 3 / 7 / (91 / 5) != 5"

    return "OK"
}