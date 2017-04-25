// EXPECTED_REACHABLE_NODES: 561
package foo

import kotlin.reflect.KClass

fun check(k: KClass<*>, instance: Any, nonInstance: Any) {
    assertTrue(k.isInstance(instance))
    assertFalse(k.isInstance(nonInstance))
    assertFalse(k.isInstance(null))
}

fun box(): String {
    check(A::class, A(), O)
    check(A::class, B(), O)
    check(O::class, O, object {})
    check(I::class, object : I {}, object {})
    check(E::class, E.X, A())
    check(E::class, E.Y, B())
    check(E::class, E.Z, O)
    check(E.Y::class, E.Y, E.X)

    return "OK"
}
