// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: sealed-classes.kt

package foo

// See KT-47376, KT-39364
@JsExport
sealed class TestSealed(val name: String) {
    class AA : TestSealed("AA") {
        fun bar(): String = "bar"
    }
    class BB : TestSealed("BB") {
        fun baz(): String = "baz"
    }

    @JsName("fromNumber")
    protected constructor(n: Int) : this(n.toString())

    protected val protectedVal = 10
    protected fun protectedFun() = 10
    protected class protectedClass {}
    protected object protectedNestedObject {}
    protected companion object {
        val companionObjectProp = 10
    }
}
