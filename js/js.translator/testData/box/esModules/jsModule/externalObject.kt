// DONT_TARGET_EXACT_BACKEND: JS
// ES_MODULES
package foo

@JsModule("./externalObject.mjs")
external object A {
    @JsName("default")
    object Default {
        val x: Int = definedExternally

        fun foo(y: Int): Int = definedExternally
    }
}

fun box(): String {
    assertEquals(23, A.Default.x)
    assertEquals(65, A.Default.foo(42))
    return "OK"
}