package foo

// CHECK_CONTAINS_NO_CALLS: test_0

internal var counter = 0

internal fun test(a: Int) {
    repeat(a) {
        counter += 1
    }
}

fun box(): String {
    assertEquals(0, counter)
    test(5)
    assertEquals(5, counter)

    return "OK"
}