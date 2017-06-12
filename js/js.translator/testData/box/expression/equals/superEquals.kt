// EXPECTED_REACHABLE_NODES: 515
package foo

open class A {
    var log = ""

    var called = false

    override fun equals(other: Any?): Boolean {
        if (called) fail("recursion detected")

        log += "A.equals;"

        called = true
        val result = super.equals(other)
        called = false
        return result
    }
}

class B : A() {
    override fun equals(other: Any?): Boolean {
        log += "B.equals;"
        if (other == null) return false
        return super.equals(other)
    }
}


fun box(): String {
    val a = A()
    testFalse { a == A() }
    assertEquals("A.equals;", a.log)

    val b1 = B()
    testTrue { b1 == b1 }
    assertEquals("B.equals;A.equals;", b1.log)

    val b2 = B()
    testFalse { b2 == B() }
    assertEquals("B.equals;A.equals;", b2.log)

    val b3 = B()
    testFalse { b3 == null }
    assertEquals("", b3.log)

    val b4 = B()
    testFalse { b4.equals(null) }
    assertEquals("B.equals;", b4.log)

    return "OK"
}
