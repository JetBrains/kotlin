// EXPECTED_REACHABLE_NODES: 502
package foo

// CHECK_NOT_CALLED: canBeCastedTo

open class A
class B
class C : A()

inline fun <reified T> Any.canBeCastedTo(): Boolean = this is T

fun box(): String {
    assertEquals(true, A().canBeCastedTo<A>(), "A().canBeCastedTo<A>()")
    assertEquals(false, A().canBeCastedTo<B>(), "A().canBeCastedTo<B>()")
    assertEquals(true, C().canBeCastedTo<A>(), "C().canBeCastedTo<A>()")

    return "OK"
}