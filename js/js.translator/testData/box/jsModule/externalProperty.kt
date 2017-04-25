// MINIFICATION_THRESHOLD: 542
// MODULE_KIND: AMD
package foo

@JsModule("lib")
external val foo: Int = definedExternally

fun box(): String {
    assertEquals(23, foo)
    return "OK"
}