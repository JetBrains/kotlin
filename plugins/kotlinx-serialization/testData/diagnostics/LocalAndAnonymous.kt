// FIR_IDENTICAL
// WITH_STDLIB
// SKIP_TXT

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

fun container() {
    @Serializable
    class X // local classes are allowed

    val y = <!ANONYMOUS_OBJECTS_NOT_SUPPORTED!>@Serializable<!> object {
        fun inObjectFun() {
            <!ANONYMOUS_OBJECTS_NOT_SUPPORTED!>@Serializable<!>
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
    class WithLocalSerializerInProperty(<!LOCAL_SERIALIZER_USAGE!>@Serializable(with = LocalSerializer::class)<!> val x: Any?)

    <!LOCAL_SERIALIZER_USAGE, SERIALIZER_TYPE_INCOMPATIBLE!>@Serializable(with = LocalSerializer::class)<!>
    data class WithLocalSerializer(val i: Int)
}

val topLevelAnon = <!ANONYMOUS_OBJECTS_NOT_SUPPORTED!>@Serializable<!> object {}

@Serializable class A {
    @Serializable class B // nested classes are allowed

    <!INNER_CLASSES_NOT_SUPPORTED!>@Serializable<!> inner class C // inner classes are not

    @Serializable object F {} // regular named object, OK
}
