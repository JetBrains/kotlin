// EXPECTED_REACHABLE_NODES: 500
package foo

class A(val value: Int) : Comparable<A> {
    override public fun compareTo(other: A): Int = other.value.compareTo(value)
}

class B(val value: Int)

fun testExtensionFunctionAsCompareTo() {
    val compareTo: B.( B ) -> Int = { other -> other.value.compareTo(this.value) }

    val x: B = B(100)
    val y: B = B(200)

    assertEquals(1, x.compareTo(y), "ext fun: x compareTo y")
}

fun testMethodAsCompareTo() {
    val x: A = A(100)
    val y: A = A(200)

    assertEquals(false, x < y, "meth: x < y")
    assertEquals(true, x > y, "meth: x > y")
    assertEquals(1, x.compareTo(y), "meth: x compareTo y")

    val comparable: Comparable<A> = x
    assertEquals(false, comparable < y, "meth: (x: Comparable<A>) < y")
    assertEquals(true, comparable > y, "meth: (x: Comparable<A>) > y")
    assertEquals(1, comparable.compareTo(y), "meth: (x: Comparable<A>) compareTo y")
}

fun box(): String {

    testExtensionFunctionAsCompareTo()

    testMethodAsCompareTo()

    return "OK"
}