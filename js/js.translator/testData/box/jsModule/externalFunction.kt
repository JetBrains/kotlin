// MODULE_KIND: AMD
package foo

@JsModule("lib")
external fun foo(y: Int): Int = noImpl

fun box(): String {
    assertEquals(65, foo(42))
    return "OK"
}