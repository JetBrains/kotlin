// WITH_STDLIB

// MODULE: lib
// FILE: libtest.kt

import kotlinx.serialization.*
import java.util.*

typealias MyDate = java.util.Date

// MODULE: main(lib)
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

// FILE: test.kt

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object MyDateSerializer : KSerializer<MyDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("uuid", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder) = TODO()

    override fun serialize(
        encoder: Encoder,
        value: MyDate
    ) = encoder.encodeString(value.toString())
}

@Serializable
data class Session(
    @Serializable(MyDateSerializer::class) val date: MyDate,
    @Serializable(MyDateSerializer::class) val alsoDate: java.util.Date,
    // The only difference with FIR is how RENDER_TYPE works:
    @Serializable(MyDateSerializer::class) val s: <!SERIALIZER_TYPE_INCOMPATIBLE("String; MyDateSerializer; MyDate /* = Date */")!>String<!>,
    val sl: List<@Serializable(MyDateSerializer::class) <!SERIALIZER_TYPE_INCOMPATIBLE("String; MyDateSerializer; MyDate /* = Date */")!>String<!>>
)

@Serializer(forClass = SessionKept::class)
object SessionKeptSerializer

@Serializable(SessionKeptSerializer::class)
@KeepGeneratedSerializer
data class SessionKept(
    @Serializable(MyDateSerializer::class) val date: MyDate,
    @Serializable(MyDateSerializer::class) val alsoDate: java.util.Date,
    // The only difference with FIR is how RENDER_TYPE works:
    @Serializable(MyDateSerializer::class) val s: <!SERIALIZER_TYPE_INCOMPATIBLE("String; MyDateSerializer; MyDate /* = Date */")!>String<!>,
val sl: List<@Serializable(MyDateSerializer::class) <!SERIALIZER_TYPE_INCOMPATIBLE("String; MyDateSerializer; MyDate /* = Date */")!>String<!>>
)
