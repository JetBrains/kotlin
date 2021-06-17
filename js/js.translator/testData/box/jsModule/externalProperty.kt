// DONT_TARGET_EXACT_BACKEND: JS
// SKIP_OLD_MODULE_SYSTEMS
package foo

@JsModule("./externalProperty.mjs")
external val foo: Int = definedExternally

fun box(): String {
    assertEquals(23, foo)
    return "OK"
}