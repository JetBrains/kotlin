package foo

// CHECK_NOT_CALLED: isInstance

trait A
trait B

open class C : A, B

class D : C()

fun box(): String {
    assertEquals(false, isInstance<A>(0))
    assertEquals(false, isInstance<A>(""))
    assertEquals(true, isInstance<A>(C()))
    assertEquals(true, isInstance<A>(D()))
    assertEquals(true, isInstance<D>(D()))
    assertEquals(true, isInstance<C>(D()))

    return "OK"
}