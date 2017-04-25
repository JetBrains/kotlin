// EXPECTED_REACHABLE_NODES: 493
package foo

// CHECK_NOT_CALLED: component1
// CHECK_NOT_CALLED: component2

class A(val a: Int, val b: Int)

inline operator fun A.component1(): Int = a
inline operator fun A.component2(): Int = b

fun box(): String {
    val (a, b) = A(1, 2)
    assertEquals(1, a)
    assertEquals(2, b)

    return "OK"
}