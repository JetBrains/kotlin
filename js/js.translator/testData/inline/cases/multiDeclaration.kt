package foo

// CHECK_NOT_CALLED: component1
// CHECK_NOT_CALLED: component2

class A(val a: Int, val b: Int)

inline fun A.component1(): Int = a
inline fun A.component2(): Int = b

fun box(): String {
    val (a, b) = A(1, 2)
    assertEquals(1, a)
    assertEquals(2, b)

    return "OK"
}