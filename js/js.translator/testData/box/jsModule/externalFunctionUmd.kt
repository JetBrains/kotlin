// EXPECTED_REACHABLE_NODES: 495
// MODULE_KIND: UMD
package foo

@JsModule("lib-foo")
@JsNonModule
external fun foo(x: Int): Int = definedExternally

fun box(): String {
    assertEquals(65, foo(42))
    return "OK"
}