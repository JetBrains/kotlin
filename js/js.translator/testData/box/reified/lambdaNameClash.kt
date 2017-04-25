// EXPECTED_REACHABLE_NODES: 499
package foo

// CHECK_CALLED: doRun
// CHECK_NOT_CALLED: test

class X
class Y

fun <R> doRun(fn: ()->R): R = fn()

inline fun <reified A, reified B> test(x: Any, y: Any): Boolean =
        doRun {
            val isA = null
            x is A
        }
        && doRun {
            val result = y is B
            val isB = null
            result
        }

fun box(): String {
    val x = X()
    val y = Y()

    assertEquals(true, test<X, Y>(x, y), "test<X, Y>(x, y)")
    assertEquals(false, test<X, Y>(x, x), "test<X, Y>(x, x)")
    assertEquals(false, test<X, Y>(y, x), "test<X, Y>(y, x)")
    assertEquals(false, test<X, Y>(y, y), "test<X, Y>(y, y)")

    return "OK"
}