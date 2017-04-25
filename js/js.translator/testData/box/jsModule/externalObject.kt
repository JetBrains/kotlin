// EXPECTED_REACHABLE_NODES: 494
// MODULE_KIND: AMD
package foo

@JsModule("lib")
external object A {
    val x: Int = definedExternally

    fun foo(y: Int): Int = definedExternally
}

fun box(): String {
    assertEquals(23, A.x)
    assertEquals(65, A.foo(42))
    return "OK"
}