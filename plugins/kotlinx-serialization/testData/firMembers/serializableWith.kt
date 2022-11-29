// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*


object IntHolderAsStringSerializer : KSerializer<IntHolder> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IntHolder", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: IntHolder) {
        encoder.encodeString(value.value.toString())
    }

    override fun deserialize(decoder: Decoder): IntHolder {
        val string = decoder.decodeString()
        return IntHolder(string.toInt())
    }
}

object ObjectSerializer : KSerializer<SerializableObject> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SerializableObject", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SerializableObject) {
        encoder.encodeString("obj")
    }

    override fun deserialize(decoder: Decoder): SerializableObject {
        decoder.decodeString()
        return SerializableObject
    }
}

@Serializable(with = IntHolderAsStringSerializer::class)
class IntHolder(val value: Int)

@Serializable(with = ObjectSerializer::class)
object SerializableObject

fun box(): String {
    val holder = IntHolder(42)

    val encoded = Json.encodeToString(IntHolder.serializer(), holder)
    if (encoded != "\"42\"") return encoded
    val decoded = Json.decodeFromString(IntHolder.serializer(), encoded)
    if (decoded.value != holder.value) return "Incorrect value"

    val encodedObject = Json.encodeToString(SerializableObject.serializer(), SerializableObject)
    if (encodedObject != "\"obj\"") return encodedObject
    val decodedObject = Json.decodeFromString(SerializableObject.serializer(), encodedObject)
    if (decodedObject != SerializableObject) return "Incorrect object instance"


    return "OK"
}
