// TARGET_BACKEND: JS_IR
// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION

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

public val publicVal = 10
public fun publicFun() = 10
public class publicClass
public external interface publicInterface

open class Class {
    internal val internalVal = 10
    internal fun internalFun() = 10
    internal class internalClass

    private val privateVal = 10
    private fun privateFun() = 10
    private class privateClass

    protected val protectedVal = 10
    protected fun protectedFun() = 10
    protected class protectedClass

    public val publicVal = 10
    public fun publicFun() = 10
    public class publicClass
}