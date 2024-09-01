// WITH_STDLIB
// API_VERSION: LATEST
// OPT_IN: kotlin.ExperimentalStdlibApi
// OPT_IN: kotlin.uuid.ExperimentalUuidApi

// FILE: serializer.kt

package kotlinx.serialization.internal

import kotlin.uuid.*
import kotlin.uuid.Uuid
import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*

// TODO: delete when serialization runtime is updated to 1.8.0
object UuidSerializer: KSerializer<Uuid> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.uuid.Uuid", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Uuid) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Uuid {
        return Uuid.parse(decoder.decodeString())
    }
}

// FILE: test.kt

package a

import kotlin.uuid.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Holder(val u: Uuid)

fun box(): String {
    return "OK"
    val h = Holder(Uuid.parse("bc501c76-d806-4578-b45e-97a264e280f1"))
    val msg = Json.encodeToString(h)
    return if (msg == """{"uuid":"bc501c76-d806-4578-b45e-97a264e280f1"}""") "OK" else "FAIL: $msg"
}

