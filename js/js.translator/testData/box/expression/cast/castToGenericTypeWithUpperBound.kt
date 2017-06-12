// EXPECTED_REACHABLE_NODES: 523
package foo

open class A
class B : A()
class C

fun <T : A> notNullToNotNullT(a: Any): T = a as T

fun <T : A> nullableToNotNullT(a: Any?): T = a as T

fun <T : A> notNullToNullableT(a: Any): T? = a as T?

fun <T : A> nullableToNullableT(a: Any?): T? = a as T?

fun box(): String {
    val a = A()
    val b = B()
    val c = C()

    success("notNullToNotNullT<A>(a)") { assertEquals(a, notNullToNotNullT<A>(a)) }
    success("notNullToNotNullT<A>(b)") { assertEquals(b, notNullToNotNullT<A>(b)) }
    failsClassCast("notNullToNotNullT<A>(c)") { notNullToNotNullT<A>(c) }

    success("nullableToNotNullT<A>(a)") {assertEquals(a, nullableToNotNullT<A>(a)) }
    success("nullableToNotNullT<A>(b)") {assertEquals(b, nullableToNotNullT<A>(b)) }
    failsClassCast("nullableToNotNullT<A>(c)") { nullableToNotNullT<A>(c) }
    failsClassCast("nullableToNotNullT<A>(null)") { nullableToNotNullT<A>(null) }

    success("notNullToNullableT<A>(a)") {  assertEquals(a, notNullToNullableT<A>(a))}
    success("notNullToNullableT<A>(b)") {  assertEquals(b, notNullToNullableT<A>(b))}
    failsClassCast("notNullToNullableT<A>(c)") { notNullToNullableT<A>(c) }

    success("nullableToNullableT<A>(a)") { assertEquals(a, nullableToNullableT<A>(a)) }
    success("nullableToNullableT<A>(b)") { assertEquals(b, nullableToNullableT<A>(b)) }
    success("nullableToNullableT<A>(null)") { assertEquals(null, nullableToNullableT<A>(null)) }
    failsClassCast("nullableToNullableT<A>(c)") { nullableToNullableT<A>(c) }

    return "OK"
}
