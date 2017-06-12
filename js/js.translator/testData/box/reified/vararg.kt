// EXPECTED_REACHABLE_NODES: 896
package foo

// CHECK_NOT_CALLED: test

class A(val x: Int)
class B(val x: Int)

inline fun <reified T> test(vararg xs: Any): List<T> {
    val ts = arrayListOf<T>()

    for (x in xs) {
        if (x is T) {
            ts.add(x)
        }
    }

    return ts
}

fun box(): String {
    val a1 = A(1)
    val b2 = B(2)
    val a3 = A(3)
    val b4 = B(4)

    assertEquals(listOf(a1), test<A>(a1, b2), "test(a1, b2)")
    assertEquals(listOf(b2, b4), test<B>(a1, b2, a3, b4), "test<B>(a1, b2, a3, b4)")

    val objects = arrayOf(a1, b2)
    assertEquals(listOf(b2, b4), test<B>(a1, a3, *objects, a3, b4), "test<B>(a1, a3, *objects, a3, b4)")

    return "OK"
}