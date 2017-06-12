// EXPECTED_REACHABLE_NODES: 492
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

fun box(): String {
    assertEquals(0, multiplyNoInline(1, 0))
    assertEquals(1, multiplyNoInline(1, 1))
    assertEquals(2, multiplyNoInline(1, 2))
    assertEquals(6, multiplyNoInline(2, 3))

    return "OK"
}