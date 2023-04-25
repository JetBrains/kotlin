// FIR_IDENTICAL
// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// WITH_STDLIB
// SKIP_TXT

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@Serializable(ExplicitSerializer::class)
data class Klass(val s: String)

object ExplicitSerializer : KSerializer<Klass> {
    override val descriptor: SerialDescriptor get() = PrimitiveSerialDescriptor("klass", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Klass) { encoder.encodeString(value.s) }
    override fun deserialize(decoder: Decoder): Klass { return Klass(decoder.decodeString()) }
}

typealias KlassAlias = Klass

@Serializable
data class DataKlass(val k: KlassAlias)