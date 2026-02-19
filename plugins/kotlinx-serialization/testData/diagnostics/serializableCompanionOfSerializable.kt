// FIR_IDENTICAL
// SKIP_TXT

// WITH_STDLIB

@file:Suppress("SERIALIZER_TYPE_INCOMPATIBLE") // to use Other as every argument

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

object Other: KSerializer<Any> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Any", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Any) {
        TODO()
    }

    override fun deserialize(decoder: Decoder): Any {
        TODO()
    }
}

@Serializable
sealed class F {
    <!COMPANION_OBJECT_IS_SERIALIZABLE_INSIDE_SERIALIZABLE_CLASS!>@Serializable<!>
    companion object {}
}

@Serializable
sealed class F2 {
    <!COMPANION_OBJECT_IS_SERIALIZABLE_INSIDE_SERIALIZABLE_CLASS!>@Serializable(Other::class)<!>
    companion object {}
}

@Serializable(Other::class)
sealed class F3 {
    <!COMPANION_OBJECT_IS_SERIALIZABLE_INSIDE_SERIALIZABLE_CLASS!>@Serializable<!>
    companion object {}
}

@Serializable(Other::class)
sealed class F4 {
    @Serializable(Other::class)
    companion object {}
}
