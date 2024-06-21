// CURIOUS_ABOUT: serializer, generatedSerializer, generatedSerializer$main
// WITH_STDLIB

// FILE: annotation.kt
package kotlinx.serialization

import kotlin.annotation.*

/*
  Until the annotation is added to the serialization runtime,
  we have to create an annotation with that name in the project itself
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KeepGeneratedSerializer

// FILE: main.kt
@file:Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE") // TODO: support common sources in the test infrastructure

import kotlin.jvm.*
import kotlin.test.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*

// == final class ==
@Serializable(with = DataSerializer::class)
@KeepGeneratedSerializer
data class Data(val i: Int)

object DataSerializer: KSerializer<Data> {
    override val descriptor = PrimitiveSerialDescriptor("DataSerializer", PrimitiveKind.INT)
    override fun deserialize(decoder: Decoder): Data {
        val value = decoder.decodeInt()
        return Data(value)
    }
    override fun serialize(encoder: Encoder, value: Data) {
        encoder.encodeInt(value.i)
    }
}

