package foo

// CHECK_NOT_CALLED: typePredicate

open class A

class B

class C : A()

interface TypePredicate {
    fun invoke(x: Any): Boolean
}

inline fun typePredicate<reified T>(): TypePredicate =
        object : TypePredicate {
            override fun invoke(x: Any): Boolean = x is T
        }

fun box(): String {
    val isA = typePredicate<A>()
    val a: Any = A()
    val b: Any = B()
    val c: Any = C()

    assertEquals(true, isA(a), "isA(a)")
    assertEquals(false, isA(b), "isA(b)")
    assertEquals(true, isA(c), "isA(c)")

    return "OK"
}