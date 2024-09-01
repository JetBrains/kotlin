// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// FILE: Md5.java

import kotlinx.serialization.Serializable;

@Serializable(with = Md5Serializer.class)
public class Md5 {
    public final String value;

    public Md5(String v) {
        value = v;
    }
}

// FILE: Md5Serializer.kt

import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*

class Md5Serializer: KSerializer<Md5> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MD5", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Md5) {
        encoder.encodeString(value.value)
    }

    override fun deserialize(decoder: Decoder): Md5 {
        return Md5(decoder.decodeString())
    }
}

// FILE: main.kt

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class A(val md5: Md5)

fun box(): String {
    val a = A(Md5("OK"))
    return Json.decodeFromString<A>(Json.encodeToString(a)).md5.value
}
