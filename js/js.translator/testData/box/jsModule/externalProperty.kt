// EXPECTED_REACHABLE_NODES: 493
// MODULE_KIND: AMD
package foo

@JsModule("lib")
external val foo: Int = definedExternally

fun box(): String {
    assertEquals(23, foo)
    return "OK"
}