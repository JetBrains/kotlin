// DONT_TARGET_EXACT_BACKEND: JS
// ES_MODULES
@file:JsImport("./externalFunction.mjs")
package foo

@JsImport.Default
external fun foo(y: Int): Int = definedExternally

fun box(): String {
    assertEquals(65, foo(42))
    return "OK"
}