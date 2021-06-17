// DONT_TARGET_EXACT_BACKEND: JS
// SKIP_OLD_MODULE_SYSTEMS
package foo

@JsModule("./externalFunction.mjs")
external fun foo(y: Int): Int = definedExternally

fun box(): String {
    assertEquals(65, foo(42))
    return "OK"
}