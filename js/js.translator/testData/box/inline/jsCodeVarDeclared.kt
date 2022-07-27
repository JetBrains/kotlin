// EXPECTED_REACHABLE_NODES: 1283
package foo

// CHECK_CONTAINS_NO_CALLS: test
// CHECK_NOT_CALLED_IN_SCOPE: function=sum scope=test

internal inline fun sum(x: Int, y: Int): Int = js("var a = x; a + y")

// CHECK_BREAKS_COUNT: function=test count=0 TARGET_BACKENDS=JS_IR
// CHECK_LABELS_COUNT: function=test name=$l$block count=0 TARGET_BACKENDS=JS_IR
internal fun test(x: Int, y: Int): Int {
    val xx = sum(x, x)
    js("var a = 0;")
    val yy = sum(y, y)

    return sum(xx, yy)
}

fun box(): String {
    assertEquals(4, test(1, 1))
    assertEquals(8, test(1, 3))

    return "OK"
}
