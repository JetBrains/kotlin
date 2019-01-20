// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1273
// SKIP_MINIFICATION
// DCE does not currently work with Closure modules.
// MODULE_KIND: CLOSURE
package foo

@JsModule("lib-foo")
@JsNonModule
external fun foo(x: Int): Int = definedExternally

fun box(): String {
    assertEquals(65, foo(42))
    return "OK"
}
