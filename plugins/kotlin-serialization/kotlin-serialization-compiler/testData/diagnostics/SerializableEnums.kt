// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE

// FILE: test.kt
import kotlinx.serialization.*

enum class SimpleEnum { A, B }

<!EXPLICIT_SERIALIZABLE_IS_REQUIRED!>enum<!> class MarkedNameEnum { @SerialName("a") A, B}

<!EXPLICIT_SERIALIZABLE_IS_REQUIRED!>enum<!> class MarkedInfoEnum { @SerialId(10) A, B}

@Serializable
enum class ExplicitlyMarkedEnum { @SerialId(10) A, B}

@Serializable(EnumSerializer::class)
enum class ExplicitlyMarkedEnumCustom { @SerialId(10) A, B}

object EnumSerializer: KSerializer<ExplicitlyMarkedEnumCustom> {
    override val descriptor = TODO()
    override fun serialize(encoder: Encoder, obj: ExplicitlyMarkedEnumCustom) = TODO()
    override fun deserialize(decoder: Decoder): ExplicitlyMarkedEnumCustom = TODO()
}