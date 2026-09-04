// WITH_STDLIB

package com.example

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*

class Data(val j: Int)

/**
 * A serializer base that already implements every `KSerializer` member, two of them `final`. The plugin must generate
 * nothing for a `@Serializer` class extending it: an override of a final method does not verify at runtime.
 */
abstract class ConcreteBase<T : Any> : KSerializer<T> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("ConcreteBase", PrimitiveKind.STRING)

    final override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString("from-base")
    }

    final override fun deserialize(decoder: Decoder): T = throw SerializationException("not needed")
}

@Serializer(forClass = Data::class)
object InheritedSerializer : ConcreteBase<Data>()

fun box(): String {
    val encoded = Json.encodeToString(InheritedSerializer, Data(1))
    if (encoded != "\"from-base\"") return encoded
    return "OK"
}
