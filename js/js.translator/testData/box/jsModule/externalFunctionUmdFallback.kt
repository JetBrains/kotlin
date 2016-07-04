// MODULE_KIND: UMD
// NO_JS_MODULE_SYSTEM
package foo

@JsModule("lib")
@JsNonModule
@native fun foo(x: Int): Int = noImpl

fun box(): String {
    assertEquals(65, foo(42))
    return "OK"
}