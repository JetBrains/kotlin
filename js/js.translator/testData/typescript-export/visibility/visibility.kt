// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_MINIFICATION
// SKIP_NODE_JS
// TODO fix statics export in DCE-driven mode
// SKIP_DCE_DRIVEN

@file:JsExport

internal val internalVal = 10
internal fun internalFun() = 10
internal class internalClass
internal external interface internalInterface

private val privateVal = 10
private fun privateFun() = 10
private class privateClass
private external interface privateInterface

public fun publicFun() = 10
public class publicClass
public external interface publicInterface

open class Class {
    // TODO: fix name clash when moving static declaration to top-level
    internal val internalVal = 10
    internal fun internalFun2() = 10
    internal class internalClass2

    private val privateVal = 10
    private fun privateFun2() = 10
    private class privateClass2

    protected val protectedVal = 10
    protected fun protectedFun2() = 10
    protected class protectedClass2

    public val publicVal = 10
    @JsName("publicFun")  // TODO: Should work without JsName
    public fun publicFun() = 10
// TODO: Fix nested classes
//    public class publicClass
}