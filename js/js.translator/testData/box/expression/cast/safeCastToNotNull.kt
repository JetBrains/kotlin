// EXPECTED_REACHABLE_NODES: 495
package foo

class A
class B

fun box(): String {
    val a: Any? = A()
    val nil: Any? = null
    val b: Any? = B()

    assertEquals(a, a as? A, "a")
    assertEquals(null, nil as? A, "nil")
    assertEquals(null, b as? A, "b")

    return "OK"
}