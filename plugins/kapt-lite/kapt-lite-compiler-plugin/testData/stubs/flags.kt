public open class PubClass {
    private companion object {}

    public class NestedPubClass
    protected class NestedProtectedClass
    internal class NestedInternalClass
    private class NestedPrivateClass

    private interface NestedPrivateIntf
    private object NestedPrivateObj

    public fun publicFun() {}
    protected fun protectedFun() {}
    internal fun internalFun() {}
    private fun privateFun() {}

    public val publicVal: Int = 0
    protected val protectedVal: Int = 0
    internal val internalVal: Int = 0
    private val privateVal: Int = 0
}

internal class InternalClass
private class PrivateClass

abstract class AbstractClass {}
open class OpenClass {}
final class FinalClass {}

private enum class PrivateEnum {
    FOO
}

private interface PrivateIntf
private object PrivateObj
private annotation class PrivateAnno