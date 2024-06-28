// FIR_IDENTICAL

package visibility

public fun publicFun(): String = ""
internal fun internalFun(): String = ""
@PublishedApi internal fun internalPAFun(): String = ""
private fun privateFun(): String = ""

public val publicVal: String get() = ""
internal val internalVal: String get() = ""
@PublishedApi internal val internalPAVal: String get() = ""
private val privateVal: String get() = ""

public var publicVarPublicSetter: String get() = ""
    set(_) = Unit
public var publicVarInternalSetter: String get() = ""
    internal set(_) = Unit
public var publicVarPrivateSetter: String get() = ""
    private set(_) = Unit
internal var internalVar: String get() = ""
    set(_) = Unit
@PublishedApi internal var internalPAVarInternalSetter: String get() = ""
    set(_) = Unit
@PublishedApi internal var internalPAVarPrivateSetter: String get() = ""
    private set(_) = Unit

public class PublicClass(val property: String) {
    fun function(): String = ""
    class NestedClass
}
public class PublicClassProtectedMembers protected constructor(protected val property: String) {
    protected fun function(): String = ""
    protected class NestedClass
}
public abstract class PublicAbstractClassProtectedMembers protected constructor(protected val property: String) {
    protected fun function(): String = ""
    protected class NestedClass
}
public open class PublicOpenClassProtectedMembers protected constructor(protected val property: String) {
    protected fun function(): String = ""
    protected class NestedClass
}
public class PublicClassInternalMembers internal constructor(internal val property: String) {
    internal fun function(): String = ""
    internal class NestedClass
}
public class PublicClassInternalPAMembers @PublishedApi internal constructor(@PublishedApi internal val property: String) {
    @PublishedApi internal fun function(): String = ""
    @PublishedApi internal class NestedClass
}
public class PublicClassPrivateMembers private constructor(private val property: String) {
    private fun function(): String = ""
    private class NestedClass
}
internal class InternalClass(val property: String) {
    fun function(): String = ""
    class NestedClass
}
@PublishedApi internal class InternalPAClass(val property: String) {
    fun function(): String = ""
    class NestedClass
}
@PublishedApi internal class InternalPAClassInternalMembers internal constructor(internal val property: String) {
    internal fun function(): String = ""
    internal class NestedClass
}
@PublishedApi internal class InternalPAClassInternalPAMembers @PublishedApi internal constructor(@PublishedApi internal val property: String) {
    @PublishedApi internal fun function(): String = ""
    @PublishedApi internal class NestedClass
}
private class PrivateClass(val property: String) {
    fun function(): String = ""
    class NestedClass
}

public typealias PublicTypeAlias = String
internal typealias InternalTypeAlias = String
private typealias PrivateTypeAlias = String
