// EXPECTED_REACHABLE_NODES: 498
package foo

// CHECK_NOT_CALLED: component2

class A(val x: Int, val y: Int)

operator fun A.component1(): Int = fizz(x)

inline operator fun A.component2(): Int = buzz(y)

fun box(): String {
    val (a, b) = A(1, 2)
    assertEquals(a, 1)
    assertEquals(b, 2)
    assertEquals("fizz(1);buzz(2);", pullLog())

    return "OK"
}