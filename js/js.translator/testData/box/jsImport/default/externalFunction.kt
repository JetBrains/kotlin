// DONT_TARGET_EXACT_BACKEND: JS
// MODULE_KIND: AMD
package foo

@JsImport("lib")
@JsImport.Default
external fun foo(y: Int): Int = definedExternally

fun box(): String {
    assertEquals(65, foo(42))
    return "OK"
}