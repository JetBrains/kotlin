// EXPECTED_REACHABLE_NODES: 498
package foo

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