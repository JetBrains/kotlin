// EXPECTED_REACHABLE_NODES: 895
package foo

// CHECK_NOT_CALLED: f1
// CHECK_NOT_CALLED: f2
// CHECK_BREAKS_COUNT: function=test count=3

internal var even = arrayListOf<Int>()
internal var odd = arrayListOf<Int>()

internal inline fun f2(x: Int): Unit {
    if (x % 2 == 0) {
        even.add(x)
        return
    }

    odd.add(x)
    return
}

internal inline fun f1(x: Boolean, y: Int, z: Int): Unit {
    if (x) {
        return f2(y)
    }

    return f2(z)
}

internal fun test(x: Boolean, y: Int, z: Int): Unit = f1(x, y, z)

fun box(): String {
    test(true, 2, 1)
    test(false, 2, 1)
    assertEquals(listOf(2), even)
    assertEquals(listOf(1), odd)

    return "OK"
}