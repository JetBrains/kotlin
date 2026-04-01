// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: abstract-classes.kt

package foo

// See KT-39364
@JsExport
abstract class TestAbstract(val name: String) {
    class AA : TestAbstract("AA") {
        fun bar(): String = "bar"
    }
    class BB : TestAbstract("BB") {
        fun baz(): String = "baz"
    }
}

@JsExport
abstract class Money<T : Money<T>> protected constructor() {
    abstract val amount: Float
    fun isZero(): Boolean = amount == 0f
}

@JsExport
class Euro(override val amount: Float) : Money<Euro>()

@JsExport
abstract class AbstractClassWithProtected {
    protected abstract fun protectedAbstractFun(): Int
    protected abstract val protectedAbstractVal: Int

    class N : AbstractClassWithProtected() {
        override fun protectedAbstractFun(): Int = 42
        override val protectedAbstractVal: Int
            get() = 42
    }
}

@JsExport
abstract class AbstractInheritorOfAbstractClass : AbstractClassWithProtected() {
    override fun protectedAbstractFun(): Int = 42
}

@JsExport
class InheritorOfAbstractClass : AbstractInheritorOfAbstractClass() {
    override val protectedAbstractVal: Int
        get() = 42
}