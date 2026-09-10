// WITH_STDLIB

import kotlinx.serialization.*

open class Wrapper<T : Wrapper<T>>

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class UnwrappingTypeSerializer<!><T : Wrapper<T>>(private val dataSerializer: KSerializer<T>) : KSerializer<Wrapper<T>>

// The serializer is written for `Wrapper<T>` and therefore takes one child serializer, but `Issue` has no
// type parameters, so the backend has nothing to pass. See KT-73207.
<!CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT, SERIALIZER_TYPE_INCOMPATIBLE!>@Serializable(UnwrappingTypeSerializer::class)<!>
data class Issue(val data: Boolean) : Wrapper<Issue>()

// Same mismatch on a property use site.
@Serializable
class Holder(<!CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT!>@Serializable(UnwrappingTypeSerializer::class)<!> val issue: <!SERIALIZER_TYPE_INCOMPATIBLE!>Issue<!>)

// Arity matches the annotated declaration — OK.
<!SERIALIZER_TYPE_INCOMPATIBLE!>@Serializable(UnwrappingTypeSerializer::class)<!>
class Ok<T : Wrapper<T>> : Wrapper<Ok<T>>()

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class NoArgsSerializer<!> : KSerializer<UsesNoArgs>

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class OneArgSerializer<!>(private val unused: KSerializer<String>) : KSerializer<UsesOneArg>

// A serializer without constructor parameters is always fine.
@Serializable(NoArgsSerializer::class)
class UsesNoArgs

// Serializer for the very same non-generic class, but with a parameter — already reported before KT-73207.
<!CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT!>@Serializable(OneArgSerializer::class)<!>
class UsesOneArg

// A secondary constructor does not help. For a serializer named by @Serializable(with = ...) the backend takes
// `constructors.single { it.owner.isPrimary }` in Instantiator.findConstructorWithoutTypeParameters and never looks at
// the others, so without this diagnostic it emits a call to the primary constructor with no arguments and fails IR
// validation with "The call provides 0 argument(s) but the called function has 1 parameter(s)".
<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class SecondaryNoArgSerializer<!>(private val unused: KSerializer<String>) : KSerializer<UsesSecondaryNoArg> {
    constructor() : this(TODO())
}

<!CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT!>@Serializable(SecondaryNoArgSerializer::class)<!>
class UsesSecondaryNoArg

// Same for a generic serializable class whose serializer has the right arity only on a secondary constructor.
<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class TwoArgSerializer<!><T>(private val a: KSerializer<T>, private val b: KSerializer<T>) : KSerializer<GenericSecondary<T>> {
    constructor(inner: KSerializer<T>) : this(inner, inner)
}

<!CUSTOM_SERIALIZER_PARAM_ILLEGAL_COUNT!>@Serializable(TwoArgSerializer::class)<!>
class GenericSecondary<T>(val t: T)

// By contrast, an @Serializer external serializer *is* instantiated through
// findSerializerConstructorForTypeArgumentsSerializers, which scans every constructor, so a secondary constructor with
// the right arity is accepted and EXTERNAL_SERIALIZER_NO_SUITABLE_CONSTRUCTOR is not reported here.
class ExternallySerializable<T>(val t: T)

@Serializer(forClass = ExternallySerializable::class)
class ExternalWithSecondaryCtor<T>(private val a: KSerializer<T>, private val b: KSerializer<T>) {
    constructor(inner: KSerializer<T>) : this(inner, inner)
}
