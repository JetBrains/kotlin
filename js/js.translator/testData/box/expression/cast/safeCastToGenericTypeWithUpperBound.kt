// EXPECTED_REACHABLE_NODES: 513
package foo

open class A
class B : A()
class C

fun <T : A> notNullToNullableT(a: Any): T? = a as? T?

fun <T : A> nullableToNullableT(a: Any?): T? = a as? T?

fun box(): String {
    val a = A()
    val b = B()
    val c = C()

    success("notNullToNullableT<A>(a)") { assertEquals(a, notNullToNullableT<A>(a)) }
    success("notNullToNullableT<A>(b)") { assertEquals(b, notNullToNullableT<A>(b)) }
    success("notNullToNullableT<A>(c)") { assertEquals(null, notNullToNullableT<A>(c)) }

    success("nullableToNullableT<A>(a)") { assertEquals(a, nullableToNullableT<A>(a)) }
    success("nullableToNullableT<A>(b)") { assertEquals(b, nullableToNullableT<A>(b)) }
    success("nullableToNullableT<A>(null)") { assertEquals(null, nullableToNullableT<A>(null)) }
    success("nullableToNullableT<A>(c)") { assertEquals(null, nullableToNullableT<A>(c)) }

    return "OK"
}
