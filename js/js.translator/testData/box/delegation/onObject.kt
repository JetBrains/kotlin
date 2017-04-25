// EXPECTED_REACHABLE_NODES: 509
package foo

interface T {
    fun foo(): String
}

class TImpl(val v: String) : T {
    override fun foo() = v
}

class A {
    companion object : T by TImpl("A.Default")
}

object B : T by TImpl("B")


fun box(): String {
    assertEquals("A.Default", A.foo())
    assertEquals("B", B.foo())

    return "OK"
}