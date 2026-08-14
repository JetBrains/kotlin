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
