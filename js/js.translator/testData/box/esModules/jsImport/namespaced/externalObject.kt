// DONT_TARGET_EXACT_BACKEND: JS
// ES_MODULES
package foo

@JsImport("./externalObject.mjs")
@JsImport.Namespace
external object A {
    object O {
        val x: Int = definedExternally
        fun foo(y: Int): Int = definedExternally
    }
}

fun box(): String {
    assertEquals(23, A.O.x)
    assertEquals(65, A.O.foo(42))
    return "OK"
}