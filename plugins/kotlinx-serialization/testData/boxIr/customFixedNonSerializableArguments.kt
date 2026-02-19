// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*

class AnyMapSerializer: KSerializer<Map<String, Any?>> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AnyMap", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Map<String, Any?>) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): Map<String, Any?> {
        return emptyMap()
    }
}

class AnyListSerializer: KSerializer<List<Any?>> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AnyList", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: List<Any?>) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): List<Any?> {
        return emptyList()
    }
}

class NonSerializable

class Box<T>

class BoxSerializerArgless(): KSerializer<Box<Any>> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("BoxSerializerArgless", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Box<Any>) = TODO()

    override fun deserialize(decoder: Decoder): Box<Any> = TODO()
}

@Serializable
data class Test(
    @Serializable(with = AnyMapSerializer::class)
    var map: Map<String, Any>?,
    @Serializable(with = AnyListSerializer::class)
    val testList: List<Any?>,
    @Serializable(with = BoxSerializerArgless::class)
    val box: Box<NonSerializable>
)

fun box(): String {
    val desc = Test.serializer().descriptor
    if (desc.getElementDescriptor(0).serialName != "AnyMap?") return "Fail MAP"
    if (desc.getElementDescriptor(1).serialName != "AnyList") return "Fail LIST"
    if (desc.getElementDescriptor(2).serialName != "BoxSerializerArgless") return "Fail BOX"
    return "OK"
}
