// WITH_STDLIB

// FILE: a.kt

package a

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

object MultiplyingIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("MultiplyingInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        return decoder.decodeInt() / 2
    }

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value * 2)
    }
}

data class Cont(val i: Int)

object ContSerializer: KSerializer<Cont> {
    override fun deserialize(decoder: Decoder): Cont {
        return Cont(decoder.decodeInt())
    }

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ContSerializer", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: Cont) {
        encoder.encodeInt(value.i)
    }
}

// FILE: test.kt

@file:UseContextualSerialization(Cont::class)
@file:UseSerializers(MultiplyingIntSerializer::class)

package a

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

@Serializable
class Holder(
    val i: Int,
    val c: Cont
)

fun testOnFile(): String {
    val j = Json {
        serializersModule = SerializersModule {
            contextual(ContSerializer)
        }
    }
    val h = Holder(3, Cont(4))
    val str = j.encodeToString(
        Holder.serializer(),
        h
    )
    if ("""{"i":6,"c":4}""" != str) return str
    val decoded = j.decodeFromString(Holder.serializer(), str)
    if (decoded.i != h.i) return "i: ${decoded.i}"
    if (decoded.c.i != h.c.i) return "c.i: ${decoded.c.i}"
    return "OK"
}

fun box(): String {
    return testOnFile()
}
