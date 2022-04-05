// EXPECTED_REACHABLE_NODES: 1283
package foo

// In the IR backend the injected JS code is outlined.
// CHECK_CONTAINS_NO_CALLS: test TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=sum scope=test

fun sum(x: Int, y: Int): Int = js("/*before x*/ x /*before +*/ + /*after +*/ y /*after y*/")

internal fun test(x: Int, y: Int): Int = sum(sum(x, x), sum(y, y))

fun box(): String {
    assertEquals(4, test(1, 1))
    assertEquals(8, test(1, 3))

    return "OK"
}