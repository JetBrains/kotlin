// DONT_TARGET_EXACT_BACKEND: JS
// MODULE_KIND: AMD
package foo

@JsImport("lib")
@JsImport.Name("bar")
external val foo: Int = definedExternally

fun box(): String {
    assertEquals(23, foo)
    return "OK"
}