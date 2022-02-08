// EXPECTED_REACHABLE_NODES: 1283
package foo

// In the IR backend the injected JS code is outlined.
// CHECK_CONTAINS_NO_CALLS: test TARGET_BACKENDS=JS
// CHECK_NOT_CALLED_IN_SCOPE: function=sum scope=test

internal inline fun sum(x: Int, y: Int): Int = js("var a = x; a + y")

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