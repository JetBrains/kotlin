// EXPECTED_REACHABLE_NODES: 499
package foo

object A {
    val x = 23
    val y = { x }
    fun foo() = { x }
}

fun box(): String {
    assertEquals(23, A.foo()())
    assertEquals(23, A.y())
    return "OK"
}