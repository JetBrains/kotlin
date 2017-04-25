// EXPECTED_REACHABLE_NODES: 492
package foo

// CHECK_CONTAINS_NO_CALLS: test except=imul
// CHECK_VARS_COUNT: function=test count=0

// A copy of stdlib run function.
// Copied to not to depend on run implementation.
// It's important, that the body is just `return fn()`.
internal inline fun <T> evaluate(fn: ()->T): T = fn()

internal fun test(x: Int): Int =
        evaluate {
            evaluate { 2 } * evaluate { x }
        }

fun box(): String {
    assertEquals(6, test(3))
    assertEquals(8, test(4))

    return "OK"
}