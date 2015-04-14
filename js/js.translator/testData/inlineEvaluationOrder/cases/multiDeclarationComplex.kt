package foo

// CHECK_NOT_CALLED: component2
// CHECK_NOT_CALLED: component3
// CHECK_NOT_CALLED: component5

class A(val a: Int, val b: Int, val c: Int, val d: Int, val e: Int)

fun A.component1(): Int = fizz(a)

inline
fun A.component2(): Int = buzz(b)

inline
fun A.component3(): Int = buzz(c)

fun A.component4(): Int = fizz(d)

inline
fun A.component5(): Int = buzz(e)

fun box(): String {
    val (a, b, c, d, e) = A(1, 2, 3, 4, 5)

    assertEquals(1, a)
    assertEquals(2, b)
    assertEquals(3, c)
    assertEquals(4, d)
    assertEquals(5, e)
    assertEquals("fizz(1);buzz(2);buzz(3);fizz(4);buzz(5);", pullLog())

    return "OK"
}