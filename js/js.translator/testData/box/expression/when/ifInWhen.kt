// EXPECTED_REACHABLE_NODES: 488
// KT-2221 if in when

package foo

fun test(caseId: Int, value: Int, expected: Int) {
    val actual: Int
    when (caseId) {
        0 -> if (value < 0) actual = -value else actual = value
        1 -> actual = if (value < 0) -value else value
        else -> throw Exception("Unexpected case: $caseId")
    }

    if (expected != actual) throw Exception("expected = $expected, actual = $actual")
}

fun box(): String {
    test(0, 3, 3)
    test(0, -13, 13)
    test(1, 23, 23)
    test(1, -3, 3)

    return "OK"
}

