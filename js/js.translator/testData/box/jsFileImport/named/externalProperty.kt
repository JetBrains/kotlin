// DONT_TARGET_EXACT_BACKEND: JS
// MODULE_KIND: AMD
@file:JsImport("lib")
package foo

@JsImport.Name("bar")
external val foo: Int = definedExternally

fun box(): String {
    assertEquals(23, foo)
    return "OK"
}