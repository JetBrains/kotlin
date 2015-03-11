package foo

// CHECK_CONTAINS_NO_CALLS: test

fun test(a: Int, b: Int): Int {
    var c = 0

    b.times {
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