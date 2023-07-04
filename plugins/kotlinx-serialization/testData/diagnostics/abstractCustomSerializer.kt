// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
// SKIP_TXT
// WITH_STDLIB

// FILE: test.kt
import kotlinx.serialization.*


interface InterfaceSerializer: KSerializer<WithInterfaceSerializer>

<!ABSTRACT_SERIALIZER_TYPE!>@Serializable(InterfaceSerializer::class)<!>
class WithInterfaceSerializer(val i: Int)


abstract class AbstractSerializer: KSerializer<WithAbstract>

<!ABSTRACT_SERIALIZER_TYPE!>@Serializable(AbstractSerializer::class)<!>
class WithAbstract(val i: Int)


sealed class SealedSerializer: KSerializer<WithSealed>

<!ABSTRACT_SERIALIZER_TYPE!>@Serializable(SealedSerializer::class)<!>
class WithSealed(val i: Int)

@Serializable
class Holder (
    <!ABSTRACT_SERIALIZER_TYPE!>@Serializable(InterfaceSerializer::class)<!>
val withInterface: WithInterfaceSerializer,

<!ABSTRACT_SERIALIZER_TYPE!>@Serializable(AbstractSerializer::class)<!>
val withAbstract: WithAbstract,

<!ABSTRACT_SERIALIZER_TYPE!>@Serializable(SealedSerializer::class)<!>
val withSealed: WithSealed
)
