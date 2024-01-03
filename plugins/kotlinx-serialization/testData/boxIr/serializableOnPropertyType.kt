// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*


@Serializable(BruhSerializerA::class)
class Bruh(val s: String)

object BruhSerializerA : KSerializer<Bruh> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Bruh", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Bruh) {
        encoder.encodeString(value.s)
    }

    override fun deserialize(decoder: Decoder): Bruh {
        return Bruh(decoder.decodeString())
    }
}

object BruhSerializerB : KSerializer<Bruh> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Bruh", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Bruh) {
        encoder.encodeString(value.s + "#")
    }

    override fun deserialize(decoder: Decoder): Bruh {
        return Bruh(decoder.decodeString().removeSuffix("#"))
    }
}

typealias BruhAlias = @Serializable(BruhSerializerB::class) Bruh

@Serializable
class Tester(
    val b1: Bruh,
    @Serializable(BruhSerializerB::class) val b2: Bruh,
    val b3: @Serializable(BruhSerializerB::class) Bruh,
    val b4: BruhAlias
)

fun box(): String {
    val t = Tester(Bruh("a"), Bruh("b"), Bruh("c"), Bruh("d"))
    val s = Json.encodeToString(t)
    if (s != """{"b1":"a","b2":"b#","b3":"c#","b4":"d#"}""") return s
    return "OK"
}
