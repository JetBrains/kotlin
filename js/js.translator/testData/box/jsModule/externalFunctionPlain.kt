// EXPECTED_REACHABLE_NODES: 1283
// SKIP_ES_MODULES
// IGNORE_BACKEND: JS_IR
package foo


@JsModule("lib")
@JsNonModule
external fun foo(y: Int): Int = definedExternally

fun box(): String {
    assertEquals(65, foo(42))
    return "OK"
}