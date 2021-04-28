// DONT_TARGET_EXACT_BACKEND: JS
package foo

@JsModule("./externalFunction.mjs")
external fun foo(y: Int): Int = definedExternally

fun box(): String {
    assertEquals(65, foo(42))
    return "OK"
}