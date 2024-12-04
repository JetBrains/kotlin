// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

// FILE: serializer.kt

package a

import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import kotlin.reflect.KClass
import kotlin.test.*

@Serializable(DataSerializer::class)
data class Data(
    val i: Int
)


@Serializer(forClass = Data::class)
private object DataSerializer


@Serializable
object SerializableObject

private object CustomSerializer : KSerializer<SerializableObject> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("My.SerializableObject", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SerializableObject) {
        encoder.encodeString("custom.serializer")
    }

    override fun deserialize(decoder: Decoder): SerializableObject {
        TODO()
    }
}

@Serializable
data class DataWithObject(
    // property is object and have custom private serializer
    @Serializable(CustomSerializer::class) val obj: SerializableObject
)

class Outer {
    private object CustomPrimitiveSerializer : KSerializer<JsonPrimitive> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("My.CustomPrimitiveSerializer", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: JsonPrimitive) {
            encoder.encodeString("custom.primitive.serializer")
        }

        override fun deserialize(decoder: Decoder): JsonPrimitive {
            TODO()
        }
    }

    fun serialize(): String {
        @Serializable
        data class Wrapper(
            // property in local class with private custom serializer
            @Serializable(CustomPrimitiveSerializer::class)
            val value: JsonPrimitive,
        )

        return Json.encodeToString(Wrapper(JsonNull))
    }
}


// FILE: holder.kt

package b

import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import kotlin.reflect.KClass
import kotlin.test.*
import a.Data
import a.DataWithObject
import a.SerializableObject
import a.Outer

@Serializable
data class Holder(
    val data: Data
)


fun box(): String {
    val json = Json.encodeToString(Holder(Data(1)))
    if (json != "{\"data\":{\"i\":1}}") return json

    val jsonWithObject = Json.encodeToString(DataWithObject(SerializableObject))
    if (jsonWithObject != "{\"obj\":\"custom.serializer\"}") return jsonWithObject

    val jsonFromOuter = Outer().serialize()
    if (jsonFromOuter != "{\"value\":\"custom.primitive.serializer\"}") return jsonFromOuter

    return "OK"
}
