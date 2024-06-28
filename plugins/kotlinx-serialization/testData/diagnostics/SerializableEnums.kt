// FIR_IDENTICAL
// WITH_STDLIB
// FILE: test.kt
import kotlinx.serialization.*
import kotlinx.serialization.encoding.*

enum class SimpleEnum { A, B }

// Annotated enums do not require @Serializable if runtime has proper factory funciton (runtime ver. >= 1.5.0)
enum class MarkedNameEnum { @SerialName("a") A, B}

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
