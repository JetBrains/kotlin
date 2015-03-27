package foo

// CHECK_CONTAINS_NO_CALLS: test

// A copy of stdlib run function.
// Copied to not to depend on run implementation.
// It's important, that the body is just `return fn()`.
inline fun evaluate<T>(fn: ()->T): T = fn()

fun test(n: Int): Int {
    return evaluate {
        var i = n
        var sum = 0

        while (i > 0) {
            sum += i
            i--
        }

        sum
    }
}

fun box(): String {
    assertEquals(6, test(3))
    assertEquals(0, test(0))
    assertEquals(0, test(-1))

    return "OK"
}