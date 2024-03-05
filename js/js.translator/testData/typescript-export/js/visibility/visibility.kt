// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_MINIFICATION
// SKIP_NODE_JS
// INFER_MAIN_MODULE

// TODO fix statics export in DCE-driven mode
// SKIP_DCE_DRIVEN

// MODULE: JS_TESTS
// FILE: visibility.kt

@JsExport
internal val internalVal = 10
@JsExport
internal fun internalFun() = 10
@JsExport
internal class internalClass
@JsExport
internal external interface internalInterface

@JsExport
private val privateVal = 10
@JsExport
private fun privateFun() = 10
@JsExport
private class privateClass
@JsExport
private external interface privateInterface

@JsExport
public val publicVal = 10
@JsExport
public fun publicFun() = 10
@JsExport
public class publicClass
@JsExport
public external interface publicInterface

@JsExport
open class Class {
    internal val internalVal = 10
    internal fun internalFun() = 10
    internal class internalClass

    private val privateVal = 10
    private fun privateFun() = 10
    private class privateClass

    protected val protectedVal = 10
    protected fun protectedFun() = 10
    protected class protectedClass {}
    protected object protectedNestedObject {}
    protected companion object {
        val companionObjectProp = 10
    }

    public class classWithProtectedConstructors protected constructor() {

        @JsName("createWithString")
        protected constructor(arg: String): this()
    }

    public val publicVal = 10
    @JsName("publicFun")  // TODO: Should work without JsName
    public fun publicFun() = 10
    public class publicClass
}

@JsExport
enum class EnumClass {
    EC1,
    EC2
}