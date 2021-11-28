// EXPECTED_REACHABLE_NODES: 1284
// ES_MODULES
// DONT_TARGET_EXACT_BACKEND: JS

package foo

@JsModule("./externalClass.mjs")
external class A(x: Int = definedExternally) {
    val x: Int

    fun foo(y: Int): Int = definedExternally

    fun bar(vararg arg: String): String = definedExternally
}

class C {
    val e = arrayOf("e")
    val f = arrayOf("f")
    val a = A(1)

    fun qux() = a.bar(*e, *f)
}


fun box(): String {
    val a = A(23)
    assertEquals(23, a.x)
    assertEquals(65, a.foo(42))

    assertEquals(C().qux(), "(ef)")

    return "OK"
}