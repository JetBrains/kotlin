// MODULE_KIND: AMD
package foo

@JsModule("lib")
@native fun foo(y: Int): Int = noImpl

fun box(): String {
    assertEquals(65, foo(42))
    return "OK"
}