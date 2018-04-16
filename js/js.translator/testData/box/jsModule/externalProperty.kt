// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1114
// MODULE_KIND: AMD
package foo

@JsModule("lib")
external val foo: Int = definedExternally

fun box(): String {
    assertEquals(23, foo)
    return "OK"
}