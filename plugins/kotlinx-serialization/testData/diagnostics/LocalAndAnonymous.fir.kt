// WITH_STDLIB
// SKIP_TXT

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

fun container() {
    @Serializable
    class X // local classes are allowed

    val y = @Serializable object {
        fun inObjectFun() {
            @Serializable
            class X // local classes in anonymous object functions are not allowed
        }
    }


    class LocalSerializer : KSerializer<Any?> {
        override val descriptor: SerialDescriptor = buildSerialDescriptor("tmp", PrimitiveKind.INT)
        override fun serialize(encoder: Encoder, value: Any?) {
            encoder.encodeNull()
        }

        override fun deserialize(decoder: Decoder): Any? {
            return decoder.decodeNull()
        }
    }

    @Serializable
    class WithLocalSerializerInProperty(@Serializable(with = LocalSerializer::class) val x: Any?)

    @Serializable(with = LocalSerializer::class)
    data class WithLocalSerializer(val i: Int)
}

val topLevelAnon = @Serializable object {}

@Serializable class A {
    @Serializable class B // nested classes are allowed

    @Serializable inner class C // inner classes are not

    @Serializable object F {} // regular named object, OK
}
