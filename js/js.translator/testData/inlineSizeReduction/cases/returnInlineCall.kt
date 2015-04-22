package foo

// CHECK_CONTAINS_NO_CALLS: test
// CHECK_VARS_COUNT: function=test count=0

inline fun sign(x: Int): Int {
    if (x < 0) return -1

    if (x == 0) return 0

    return 1
}

fun test(x: Int): Int = sign(x)

fun box(): String {
    assertEquals(-1, test(-2))
    assertEquals(0, test(0))
    assertEquals(1, test(2))

    return "OK"
}