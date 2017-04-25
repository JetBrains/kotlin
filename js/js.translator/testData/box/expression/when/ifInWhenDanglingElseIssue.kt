// EXPECTED_REACHABLE_NODES: 488
// http://youtrack.jetbrains.com/issue/KT-5253
// JS: generated wrong code when use `if` inside `when`

package foo


fun test(caseId: Int, value: Int, expected: Int) {
    var actual: Int = 0
    when (caseId) {
        2 -> if (value < 0) actual = -value
        3 -> actual = if (value < 0) {-value} else value
        else -> throw Exception("Unexpected case: $caseId")
    }

    if (expected != actual) throw Exception("expected = $expected, actual = $actual")
}


fun box(): String {

    test(2, 33, 0)
    test(2, -1, 1)
    test(3, 23, 23)
    test(3, -3, 3)

    return "OK"
}