// MODULE_KIND: AMD
package foo

@JsModule("lib")
external val foo: Int = noImpl

fun box(): String {
    assertEquals(23, foo)
    return "OK"
}