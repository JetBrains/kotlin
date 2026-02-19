// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import java.util.Date

class NotSerializable(val i: Int)

object SerializerB : KSerializer<NotSerializable> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("SerializerB", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: NotSerializable) = TODO()

    override fun deserialize(decoder: Decoder): NotSerializable = TODO()
}

object DateSer : KSerializer<Date> {
    override val descriptor = PrimitiveSerialDescriptor("DateSer", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Date = TODO()

    override fun serialize(encoder: Encoder, value: Date) = TODO()
}

typealias S = @Serializable(SerializerB::class) NotSerializable
typealias BS = @Serializable(DateSer::class) Date

@Serializable
class Foo(
    val s: S,
    val bs: BS,
    val list: List<BS>
)

fun box(): String {
    val list = Foo.serializer().descriptor.elementDescriptors.map { it.serialName }.toList()
    if (list != listOf("SerializerB", "DateSer", "kotlin.collections.ArrayList")) return list.toString()
    return "OK"
}
