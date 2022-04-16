// EXPECTED_REACHABLE_NODES: 1283
package foo

// CHECK_CONTAINS_NO_CALLS: multiplyNoInline except=imul

internal inline fun multiply(a: Int, b: Int): Int {
    return a * b
}

internal fun multiplyNoInline(a: Int, b: Int): Int {
    var c = a - 1
    var d = b - 1
    return (c++) + (d++) + multiply(c, d) - (--c + --d)
}

// CHECK_BREAKS_COUNT: function=box count=0 TARGET_BACKENDS=JS_IR
// CHECK_LABELS_COUNT: function=box name=$l$block count=0 TARGET_BACKENDS=JS_IR
fun box(): String {
    assertEquals(0, multiplyNoInline(1, 0))
    assertEquals(1, multiplyNoInline(1, 1))
    assertEquals(2, multiplyNoInline(1, 2))
    assertEquals(6, multiplyNoInline(2, 3))

    return "OK"
}