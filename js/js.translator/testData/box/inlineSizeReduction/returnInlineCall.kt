package foo

// CHECK_CONTAINS_NO_CALLS: test_0
// CHECK_VARS_COUNT: function=test_0 count=0

internal inline fun sign(x: Int): Int {
    if (x < 0) return -1

    if (x == 0) return 0

    return 1
}

internal fun test(x: Int, y: Int): Int {
    if (x != 0) {
        return sign(x)
    }

    return sign(y)
}

fun box(): String {
    assertEquals(-1, test(-2, 2))
    assertEquals(1, test(2, -2))
    assertEquals(-1, test(0, -2))
    assertEquals(1, test(0, 2))
    assertEquals(0, test(0, 0))

    return "OK"
}