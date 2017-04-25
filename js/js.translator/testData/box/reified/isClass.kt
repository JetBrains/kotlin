// EXPECTED_REACHABLE_NODES: 504
package foo

// CHECK_NOT_CALLED: isInstance

interface A
interface B

open class C : A, B

class D : C()

fun box(): String {
    assertEquals(false, isInstance<A>(0))
    assertEquals(false, isInstance<A>(""))
    assertEquals(true, isInstance<A>(C()))
    assertEquals(true, isInstance<A>(D()))
    assertEquals(true, isInstance<D>(D()))
    assertEquals(true, isInstance<C>(D()))

    assertEquals(true, isInstance<D?>(D()), "isInstance<D?>(D())")
    assertEquals(true, isInstance<D?>(null), "isInstance<D?>(null)")
    assertEquals(false, isInstance<D?>(C()), "isInstance<D?>(C())")

    return "OK"
}