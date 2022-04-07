// EXPECTED_REACHABLE_NODES: 1283
package foo

// In the IR backend the injected JS code is outlined.
// CHECK_CONTAINS_NO_CALLS: test TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=sum scope=test

internal inline fun sum(x: Int, y: Int): Int = js("x + y")

// CHECK_BREAKS_COUNT: function=test count=0 TARGET_BACKENDS=JS_IR
// CHECK_LABELS_COUNT: function=test name=$l$block count=0 TARGET_BACKENDS=JS_IR
internal fun test(x: Int, y: Int): Int = sum(sum(x, x), sum(y, y))

fun box(): String {
    assertEquals(4, test(1, 1))
    assertEquals(8, test(1, 3))

    return "OK"
}