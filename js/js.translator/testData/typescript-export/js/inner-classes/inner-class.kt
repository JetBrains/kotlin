// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
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

    open inner class OpenInnerWithPublicConstructor(val a: String) {
        @JsName("fromNumber")
        constructor(a: Int) : this(a.toString())

        val concat: String = this@TestInner.a + this.a
    }

    open inner class OpenInnerWithProtectedConstructor protected constructor(val a: String) {
        @JsName("fromNumber")
        protected constructor(a: Int) : this(a.toString())

        val concat: String = this@TestInner.a + this.a
    }
}