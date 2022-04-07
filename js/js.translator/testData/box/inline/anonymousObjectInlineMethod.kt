// EXPECTED_REACHABLE_NODES: 1287
package foo

// CHECK_BREAKS_COUNT: function=add count=0 TARGET_BACKENDS=JS_IR
// CHECK_LABELS_COUNT: function=add name=$l$block count=0 TARGET_BACKENDS=JS_IR
fun add(a: Int, b: Int): Int {
    val o = object {
        inline fun add(a: Int, b: Int): Int = a + b
    }

    return o.add(a, b)
}

fun box(): String {
    assertEquals(3, add(1, 2))
    assertEquals(5, add(2, 3))

    return "OK"
}