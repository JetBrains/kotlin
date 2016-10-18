package foo

fun box(): String {
    check(A::class, A().jsClass.kotlin)
    check(B::class, B().jsClass.kotlin)
    check(O::class, (O).jsClass.kotlin)
    check(E::class, E.X.jsClass.kotlin)
    check(E::class, E.Y.jsClass.kotlin, shouldBeEqual = false)
// TODO uncomment after KT-13338 is fixed
//    check(E::class, E.Z.jsClass.kotlin)

    return "OK"
}
