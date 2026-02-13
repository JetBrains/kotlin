// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// INFER_MAIN_MODULE

// TODO fix statics export in DCE-driven mode
// SKIP_DCE_DRIVEN

// MODULE: JS_TESTS
// FILE: visibility.kt

<!WRONG_JS_EXPORT_TARGET_VISIBILITY!>@JsExport<!>
internal val internalVal = 10
<!WRONG_JS_EXPORT_TARGET_VISIBILITY!>@JsExport<!>
internal fun internalFun() = 10
<!WRONG_JS_EXPORT_TARGET_VISIBILITY!>@JsExport<!>
internal class internalClass
<!WRONG_JS_EXPORT_TARGET_VISIBILITY!>@JsExport<!>
internal external interface internalInterface

<!WRONG_JS_EXPORT_TARGET_VISIBILITY!>@JsExport<!>
private val privateVal = 10
<!WRONG_JS_EXPORT_TARGET_VISIBILITY!>@JsExport<!>
private fun privateFun() = 10
<!WRONG_JS_EXPORT_TARGET_VISIBILITY!>@JsExport<!>
private class privateClass
<!WRONG_JS_EXPORT_TARGET_VISIBILITY!>@JsExport<!>
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
abstract class AbstractClassWithProtected {
    protected abstract fun protectedAbstractFun(): Int
    protected abstract val protectedAbstractVal: Int
}

@JsExport
open class Class: AbstractClassWithProtected() {
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

    override fun protectedAbstractFun(): Int = 10
    override val protectedAbstractVal: Int
        get() = 10
}

@JsExport
class FinalClass protected constructor(): AbstractClassWithProtected() {

    @JsName("fromNumber")
    protected constructor(n: Int) : this()

    @JsName("fromString")
    constructor(s: String) : this()

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

@JsExport
class FinalClassWithPublicPrimaryProtectedSecondaryCtor(s: String) {
    @JsName("fromInt")
    protected constructor(n: Int): this(n.toString())
}

@JsExport
class FinalClassWithProtectedPrimaryPublicSecondaryCtor protected constructor(s: String) {
    @JsName("fromInt")
    public constructor(n: Int): this(n.toString())
}

@JsExport
class FinalClassWithOnlySecondaryCtorsMixedVisibility {
    @JsName("fromInt")
    protected constructor(n: Int)

    @JsName("fromString")
    public constructor(s: String)
}

@JsExport
enum class EnumClass {
    EC1,
    EC2
}