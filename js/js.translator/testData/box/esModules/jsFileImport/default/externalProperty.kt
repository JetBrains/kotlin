// DONT_TARGET_EXACT_BACKEND: JS
// ES_MODULES
@file:JsImport("./externalProperty.mjs")
package foo

@JsImport.Default
external val foo: Int = definedExternally

fun box(): String {
    assertEquals(23, foo)
    return "OK"
}