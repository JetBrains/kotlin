package foo

fun testWithInstance() {
    assertEquals("A", A().jsClass.name)
    assertEquals("B", B().jsClass.name)
    assertEquals("O", O.jsClass.name)
    assertEquals("E", E.X.jsClass.name)
    assertEquals("Y", E.Y.jsClass.name)
// TODO uncomment after KT-13338 is fixed
//    assertEquals("E", E.Z.jsClass.name)
    assertEquals("Q", R().jsClass.name)
}

fun testWithClassReference() {
    assertEquals("A", jsClass<A>().name)
    assertEquals("B", jsClass<B>().name)
    assertEquals("O", jsClass<O>().name)
    assertEquals("I", jsClass<I>().name)
    assertEquals("E", jsClass<E>().name)
    assertEquals("Q", jsClass<R>().name)
}

fun box(): String {
    testWithInstance()
    testWithClassReference()

    return "OK"
}
