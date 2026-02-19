// WITH_STDLIB

@file:Suppress("SERIALIZER_TYPE_INCOMPATIBLE") // to use Other as every argument

import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*

object Other: KSerializer<Any> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Any", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Any) {
        TODO()
    }

    override fun deserialize(decoder: Decoder): Any {
        TODO()
    }
}

sealed class NonSerializable {
    @Serializable(Other::class)
    companion object: NonSerializable()
}

@Serializable(Other::class)
sealed class SerializableOther {
    @Serializable(Other::class)
    companion object : SerializableOther() {}
}

fun box(): String {
    val s0 = NonSerializable.serializer().descriptor.serialName
    val s1 = SerializableOther.serializer().descriptor.serialName
    if (s0 != "Any") return s0
    if (s1 != "Any") return s1
    return "OK"
}
