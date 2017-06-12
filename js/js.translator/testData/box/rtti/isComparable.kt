// EXPECTED_REACHABLE_NODES: 502
package foo

class A : Comparable<A> {
    override fun compareTo(other: A): Int = 0
}

class B

fun test(x: Any?): Boolean = x is Comparable<*>

fun box(): String {
    assertEquals(true, test(A()), "A()")
    assertEquals(true, test("abc"), "\"abc\"")
    assertEquals(true, test('a'), "\'a\'")
    assertEquals(true, test(0), "0")
    assertEquals(true, test(0.toChar()), "0.toChar()")
    assertEquals(true, test(0.toByte()), "0.toByte()")
    assertEquals(true, test(0.toShort()), "0.toShort()")
    assertEquals(true, test(0.toLong()), "0.toLong()")
    assertEquals(true, test(0.toDouble()), "0.toDouble()")
    assertEquals(true, test(0.toFloat()), "0.toFloat()")
    assertEquals(false, test(B()), "B()")

    return "OK"
}