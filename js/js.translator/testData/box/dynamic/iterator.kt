// EXPECTED_REACHABLE_NODES: 1224
package foo

fun testFor(expected: Int, d: dynamic, case: String) {
    var actual = 0
    for (v in d) {
        actual += v as Int
    }
    assertEquals(expected, actual, "testFor on $case")
}

fun testIterator(expected: Int, d: dynamic, case: String) {
    var actual = 0
    val it = d.iterator()
    while (it.hasNext()) {
        actual += it.next() as Int
    }
    assertEquals(expected, actual, "testIterator on $case")
}

fun test(expected: Int, d: dynamic, case: String) {
    testFor(expected, d, case)
    testIterator(expected, d, case)
}

fun box(): String {
    test(6, arrayOf(1, 2, 3), "array")
    test(64, byteArrayOf(42, 22), "byte array")
    test(66, listOf(55, 3, 8), "list")
    test(167, setOf(55, 3, 8, 101), "set")

    return "OK"
}