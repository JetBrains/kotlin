// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE

// FILE: test.kt
import kotlinx.serialization.*

enum class SimpleEnum { A, B }

<!PLUGIN_WARNING("Explicit @Serializable annotation on enum class is required when @SerialName or @SerialInfo annotations are used on its members.")!>enum<!> class MarkedNameEnum { @SerialName("a") A, B}

<!PLUGIN_WARNING("Explicit @Serializable annotation on enum class is required when @SerialName or @SerialInfo annotations are used on its members.")!>enum<!> class MarkedInfoEnum { @SerialId(10) A, B}

@Serializable
enum class ExplicitlyMarkedEnum { @SerialId(10) A, B}

@Serializable(EnumSerializer::class)
enum class ExplicitlyMarkedEnumCustom { @SerialId(10) A, B}

object EnumSerializer: KSerializer<ExplicitlyMarkedEnumCustom> {
    override val descriptor = TODO()
    override fun serialize(encoder: Encoder, obj: ExplicitlyMarkedEnumCustom) = TODO()
    override fun deserialize(decoder: Decoder): ExplicitlyMarkedEnumCustom = TODO()
}