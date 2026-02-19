// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: sealed-classes.kt

package foo

@JsExport
abstract class AbstractClassWithProtected {
    protected abstract fun protectedAbstractFun(): Int
    protected abstract val protectedAbstractVal: Int
}

// See KT-47376, KT-39364
@JsExport
sealed class TestSealed(val name: String) : AbstractClassWithProtected() {
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

    override fun protectedAbstractFun(): Int = 10
    override val protectedAbstractVal: Int
        get() = 10
}
