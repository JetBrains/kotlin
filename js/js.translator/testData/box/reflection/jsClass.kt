// EXPECTED_REACHABLE_NODES: 551
package foo

fun box(): String {
    check(jsClass<A>(), A().jsClass)
    check(jsClass<B>(), B().jsClass)
    check(jsClass<O>(), O.jsClass)
    assertNotEquals(null, jsClass<I>())
    check(jsClass<E>(), E.X.jsClass)
    check(jsClass<E>(), E.Y.jsClass, shouldBeEqual = false)
// TODO uncomment after KT-13338 is fixed
//    check(jsClass<E>(), E.Z.jsClass)

    return "OK"
}
