// WITH_STDLIB

// MODULE: lib
// FILE: libtest.kt

import kotlinx.serialization.*
import java.util.*

typealias MyDate = java.util.Date

// MODULE: main(lib)
// FILE: test.kt

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
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
    @Serializable(MyDateSerializer::class) val s: <!SERIALIZER_TYPE_INCOMPATIBLE("kotlin.String; MyDateSerializer; java.util.Date")!>String<!>,
    val sl: List<<!SERIALIZER_TYPE_INCOMPATIBLE("@Serializable(...) kotlin.String; MyDateSerializer; java.util.Date")!>@Serializable(MyDateSerializer::class) String<!>>
)
