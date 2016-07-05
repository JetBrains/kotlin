// MODULE_KIND: UMD
// NO_JS_MODULE_SYSTEM
package foo

@JsModule("libfoo")
@JsNonModule
@native fun foo(x: Int): Int = noImpl

@JsModule("libbar") 
@JsNonModule
@JsName("baz")
@native fun bar(x: Int): Int = noImpl

fun box(): String {
    assertEquals(65, foo(42))
    assertEquals(142, bar(100))
    return "OK"
}