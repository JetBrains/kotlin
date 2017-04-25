// EXPECTED_REACHABLE_NODES: 493
// MODULE_KIND: AMD
package foo

@JsModule("lib")
external class A(x: Int = definedExternally) {
    val x: Int

    fun foo(y: Int): Int = definedExternally
}

fun box(): String {
    val a = A(23)
    assertEquals(23, a.x)
    assertEquals(65, a.foo(42))

    return "OK"
}