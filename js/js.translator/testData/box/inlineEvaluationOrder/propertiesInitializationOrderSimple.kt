// EXPECTED_REACHABLE_NODES: 497
package foo

class A {
    val x: Int

    init {
        x = fizz(1) + buzz(2)
    }
}

fun box(): String {
    val a = A()
    assertEquals(3, a.x)
    assertEquals("fizz(1);buzz(2);", pullLog())

    return "OK"
}