package foo

// CHECK_CONTAINS_NO_CALLS: test_0

internal fun test(a: Int, b: Int): Int {
    var c = 0

    repeat(b) {
        c += a
    }

    return c
}

fun box(): String {
    assertEquals(6, test(2, 3))
    assertEquals(6, test(3, 2))
    assertEquals(20, test(4, 5))

    return "OK"
}