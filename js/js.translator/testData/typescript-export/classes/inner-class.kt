// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_MINIFICATION
// SKIP_NODE_JS
// IGNORE_BACKEND: JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: inner-class.kt

package foo

@JsExport
class TestInner(val a: String) {
    inner class Inner(val a: String) {
        val concat: String = this@TestInner.a + this.a

        @JsName("fromNumber")
        constructor(a: Int): this(a.toString())

        @JsName("SecondLayerInner")
        inner class InnerInner(val a: String) {
            val concat: String = this@TestInner.a + this@Inner.a + this.a
        }
    }
}