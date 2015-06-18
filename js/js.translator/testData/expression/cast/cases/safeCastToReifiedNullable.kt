package foo

// CHECK_NOT_CALLED: castTo

class A
class B

inline
fun Any?.castTo<reified T>(): T? = this as? T?

fun box(): String {
    val a: Any? = A()
    val nil: Any? = null
    val b: Any? = B()

    assertEquals(a, a.castTo<A>(), "a")
    assertEquals(null, nil.castTo<A>(), "nil")
    assertEquals(null, b.castTo<A>(), "b")

    return "OK"
}