// DONT_TARGET_EXACT_BACKEND: JS
// MODULE_KIND: AMD
@file:JsImport("lib")
package foo

@JsImport.Default
external fun foo(y: Int): Int = definedExternally

fun foo(y: String): String = y + "K"

fun box(): String {
    val foo10 = foo(10)
    if (foo10 != 33) return "Fail: $foo10"
    return "OK"
}