// EXPECTED_REACHABLE_NODES: 497
package foo

class A(val x: Int = fizz(1) + 1) {
    val y = buzz(x) + 1
    val z: Int

    init {
        z = fizz(x) + buzz(y)
    }
}

fun box(): String {
    val a = A()
    assertEquals(2, a.x)
    assertEquals(3, a.y)
    assertEquals(5, a.z)
    assertEquals("fizz(1);buzz(2);fizz(2);buzz(3);", pullLog())

    return "OK"
}