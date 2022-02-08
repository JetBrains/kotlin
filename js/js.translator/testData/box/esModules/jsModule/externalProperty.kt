// DONT_TARGET_EXACT_BACKEND: JS
// ES_MODULES
package foo

@JsModule("./externalProperty.mjs")
external val foo: Int = definedExternally

fun box(): String {
    assertEquals(23, foo)
    return "OK"
}