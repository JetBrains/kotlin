// WITH_STDLIB

package com.example

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*


@Serializable(WithCompanion.Companion::class)
data class WithCompanion(val i: Int) {
    @Serializer(forClass = WithCompanion::class)
    companion object
}

@Serializable(WithNamedCompanion.Named::class)
data class WithNamedCompanion(val i: Int) {
    @Serializer(forClass = WithNamedCompanion::class)
    companion object Named
}

@Serializable(WithExplicitType.Companion::class)
data class WithExplicitType(val i: Int) {
    @Serializer(forClass = WithExplicitType::class)
    companion object : KSerializer<WithExplicitType>
}

@Serializable(PartiallyOverridden.Companion::class)
data class PartiallyOverridden(val i: Int) {
    @Serializer(forClass = PartiallyOverridden::class)
    companion object : KSerializer<PartiallyOverridden> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Partially-Overridden") {
            element("i", PrimitiveSerialDescriptor("i", PrimitiveKind.INT))
        }

        override fun serialize(encoder: Encoder, value: PartiallyOverridden) {
            val compositeOutput = encoder.beginStructure(PartiallyOverridden.descriptor)
            compositeOutput.encodeIntElement(PartiallyOverridden.descriptor, 0, value.i + 10)
            compositeOutput.endStructure(PartiallyOverridden.descriptor)
        }
    }
}

@Serializable(PartiallyWithoutType.Companion::class)
data class PartiallyWithoutType(val i: Int) {
    @Serializer(forClass = PartiallyWithoutType::class)
    companion object {

        override fun deserialize(decoder: Decoder): PartiallyWithoutType {
            val dec: CompositeDecoder = decoder.beginStructure(descriptor)
            var iv: Int? = null
            loop@ while (true) {
                when (val i = dec.decodeElementIndex(descriptor)) {
                    CompositeDecoder.DECODE_DONE -> break@loop
                    0 -> iv = dec.decodeIntElement(descriptor, i)
                    else -> throw SerializationException("Unknown index $i")
                }
            }
            dec.endStructure(descriptor)
            return PartiallyWithoutType(iv!! + 10)
        }
    }
}


@Serializable(FullyOverridden.Companion::class)
data class FullyOverridden(val i: Int) {
    companion object : KSerializer<FullyOverridden> {

        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FullyOverridden", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: FullyOverridden) {
            encoder.encodeString("i=${value.i}")
        }

        override fun deserialize(decoder: Decoder): FullyOverridden {
            val i = decoder.decodeString().substringAfter('=').toInt()
            return FullyOverridden(i)
        }
    }
}


fun box(): String {
    encodeAndDecode(WithCompanion.serializer(), WithCompanion(1), """{"i":1}""")?.let { return it }

    encodeAndDecode(WithNamedCompanion.serializer(), WithNamedCompanion(2), """{"i":2}""")?.let { return it }

    encodeAndDecode(WithExplicitType.serializer(), WithExplicitType(3), """{"i":3}""")?.let { return it }

    encodeAndDecode(FullyOverridden.serializer(), FullyOverridden(4), "\"i=4\"")?.let { return it }

    encodeAndDecode(PartiallyOverridden.serializer(), PartiallyOverridden(5), """{"i":15}""", PartiallyOverridden(15))?.let { return it }
    if (PartiallyOverridden.serializer().descriptor.serialName != "Partially-Overridden") return PartiallyOverridden.serializer().descriptor.serialName

    encodeAndDecode(PartiallyWithoutType.serializer(), PartiallyWithoutType(6), """{"i":6}""", PartiallyWithoutType(16))?.let { return it }
    if (PartiallyWithoutType.serializer().descriptor.serialName != PartiallyWithoutType::class.qualifiedName) return PartiallyWithoutType.serializer().descriptor.serialName

    return "OK"
}


private fun <T> encodeAndDecode(serializer: KSerializer<T>, value: T, expectedEncoded: String, expectedDecoded: T? = null): String? {
    val encoded = Json.encodeToString(serializer, value)
    if (encoded != expectedEncoded) return encoded

    val decoded = Json.decodeFromString(serializer, encoded)
    if (decoded != (expectedDecoded ?: value)) return "DECODED=${decoded.toString()}"
    return null
}
