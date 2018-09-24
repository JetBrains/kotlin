// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1328
package foo

@Suppress("DEPRECATION_ERROR")
fun testWithInstance() {
    assertEquals("A", A().jsClass.name)
    assertEquals("B", B().jsClass.name)
    assertEquals("O", O.jsClass.name)
    assertEquals("E", E.X.jsClass.name)
    assertEquals("E\$Y", E.Y.jsClass.name)
// TODO uncomment after KT-13338 is fixed
//    assertEquals("E", E.Z.jsClass.name)
    assertEquals("R", R().jsClass.name)
}

@Suppress("DEPRECATION_ERROR")
fun testWithClassReference() {
    assertEquals("A", jsClass<A>().name)
    assertEquals("B", jsClass<B>().name)
    assertEquals("O", jsClass<O>().name)
    assertEquals("I", jsClass<I>().name)
    assertEquals("E", jsClass<E>().name)
    assertEquals("R", jsClass<R>().name)
}

fun box(): String {
    testWithInstance()
    testWithClassReference()

    return "OK"
}
