package foo

fun <T> check(x: JsClass<T>, y: JsClass<T>, shouldBeEquals: Boolean = true) {
    assertNotEquals(null, x)
    assertNotEquals(null, y)
    if (shouldBeEquals)
        assertEquals(x, y)
    else
        assertNotEquals(x, y)
}

fun box(): String {
    check(jsClass<A>(), A().jsClass)
    check(jsClass<B>(), B().jsClass)
    check(jsClass<O>(), O.jsClass)
    assertNotEquals(null, jsClass<I>())
    check(jsClass<E>(), E.X.jsClass)
    check(jsClass<E>(), E.Y.jsClass, shouldBeEquals = false)
// TODO uncomment after KT-13338 is fixed
//    check(jsClass<E>(), E.Z.jsClass)

    return "OK"
}
