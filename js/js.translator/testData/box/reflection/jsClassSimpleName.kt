// EXPECTED_REACHABLE_NODES: 559
package foo

class undefined

class Outer {
    class Nested

    inner class Inner
}

fun testWithInstance() {
    assertEquals("A", A().jsClass.simpleName)
    assertEquals("B", B().jsClass.simpleName)
    assertEquals("O", O.jsClass.simpleName)
    assertEquals("E", E.X.jsClass.simpleName)
    assertEquals("Y", E.Y.jsClass.simpleName)
// TODO uncomment after KT-13338 is fixed
//    assertEquals("E", E.Z.jsClass.simpleName)
    assertEquals("undefined", undefined().jsClass.simpleName)
    assertEquals("Nested", Outer.Nested().jsClass.simpleName)
    assertEquals("Inner", Outer().Inner().jsClass.simpleName)
}

fun testWithClassReference() {
    assertEquals("A", jsClass<A>().simpleName)
    assertEquals("B", jsClass<B>().simpleName)
    assertEquals("O", jsClass<O>().simpleName)
    assertEquals("I", jsClass<I>().simpleName)
    assertEquals("E", jsClass<E>().simpleName)
    assertEquals("undefined", jsClass<undefined>().simpleName)
    assertEquals("Nested", jsClass<Outer.Nested>().simpleName)
    assertEquals("Inner", jsClass<Outer.Inner>().simpleName)
}

val JsClass<*>.simpleName: String
    get() {
        val dynClass: dynamic = this
        return dynClass.`$metadata$`.simpleName as? String ?: ""
    }

fun box(): String {
    testWithInstance()
    testWithClassReference()

    return "OK"
}
