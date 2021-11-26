// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE
// WITH_STDLIB
// FILE: test.kt
import kotlinx.serialization.*
import kotlinx.serialization.encoding.*

enum class SimpleEnum { A, B }

<!EXPLICIT_SERIALIZABLE_IS_REQUIRED!>enum<!> class MarkedNameEnum { @SerialName("a") A, B}

@Serializable
enum class ExplicitlyMarkedEnum { @SerialName("a") A, B}

@Serializable(EnumSerializer::class)
enum class ExplicitlyMarkedEnumCustom { @SerialName("a") A, B}

object EnumSerializer: KSerializer<ExplicitlyMarkedEnumCustom> {
    override val descriptor = TODO()
    override fun serialize(encoder: Encoder, value: ExplicitlyMarkedEnumCustom) = TODO()
    override fun deserialize(decoder: Decoder): ExplicitlyMarkedEnumCustom = TODO()
}

@Serializable
data class EnumUsage(val s: SimpleEnum, val m: MarkedNameEnum, val e: ExplicitlyMarkedEnum)