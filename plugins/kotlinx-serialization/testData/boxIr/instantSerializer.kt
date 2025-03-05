// WITH_STDLIB
// API_VERSION: LATEST
// OPT_IN: kotlin.time.ExperimentalTime

// FILE: serializer.kt

package kotlinx.serialization.internal

import kotlin.time.*
import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*

// TODO: delete when serialization runtime is updated to 1.9.0
internal object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.time.Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString())
    }
}

// FILE: test.kt

package a

import kotlin.time.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Holder(val i: Instant)

fun box(): String {
    return "OK"
    val h = Holder(Instant.parse("2025-01-04T23:59:14.0001242Z"))
    val msg = Json.encodeToString(h)
    return if (msg == """{"instant":"2025-01-04T23:59:14.0001242Z"}""") "OK" else "FAIL: $msg"
}

