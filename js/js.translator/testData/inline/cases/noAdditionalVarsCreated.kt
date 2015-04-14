package foo

// CHECK_CONTAINS_NO_CALLS: test
// COUNT_VARS: function=test count=0

// A copy of stdlib run function.
// Copied to not to depend on run implementation.
// It's important, that the body is just `return fn()`.
inline fun evaluate<T>(fn: ()->T): T = fn()

fun test(x: Int): Int =
        evaluate {
            evaluate { 2 } * evaluate { x }
        }

fun box(): String {
    assertEquals(6, test(3))
    assertEquals(8, test(4))

    return "OK"
}