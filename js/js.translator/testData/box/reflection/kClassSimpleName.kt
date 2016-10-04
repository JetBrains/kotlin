package foo

class undefined

class Outer {
    class Nested

    inner class Inner
}

fun testWithInstance() {
    assertEquals("A", A()::class.simpleName)
    assertEquals("B", B()::class.simpleName)
    assertEquals("O", (O)::class.simpleName)
    assertEquals("E", E.X::class.simpleName)
    assertEquals("Y", E.Y::class.simpleName)
// TODO uncomment after KT-13338 is fixed
//    assertEquals("E", E.Z::class.simpleName)
    assertEquals("undefined", undefined()::class.simpleName)
    assertEquals("Nested", Outer.Nested()::class.simpleName)
    assertEquals("Inner", Outer().Inner()::class.simpleName)
}

fun testWithClassReference() {
    assertEquals("A", A::class.simpleName)
    assertEquals("B", B::class.simpleName)
    assertEquals("O", O::class.simpleName)
    assertEquals("I", I::class.simpleName)
    assertEquals("E", E::class.simpleName)
    assertEquals("undefined", foo.undefined::class.simpleName)
    assertEquals("Nested", Outer.Nested::class.simpleName)
    assertEquals("Inner", Outer.Inner::class.simpleName)
}

fun box(): String {
    testWithInstance()
    testWithClassReference()

    return "OK"
}
