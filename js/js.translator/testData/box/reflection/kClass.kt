// EXPECTED_REACHABLE_NODES: 552
package foo

fun box(): String {
    check(A::class, A()::class)
    check(B::class, B()::class)
    check(O::class, (O)::class)
    assertNotEquals(null, I::class)
    check(E::class, E.X::class)
    check(E::class, E.Y::class, shouldBeEqual = false)
// TODO uncomment after KT-13338 is fixed
//    check(E::class, E.Z::class)

    return "OK"
}
