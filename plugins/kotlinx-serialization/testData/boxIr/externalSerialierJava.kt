// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import java.net.URL


// @Serializer should do nothing if all methods are overriden
@Serializer(forClass = URL::class)
object URLSerializer : KSerializer<URL> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.net.URL", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: URL) {
        TODO()
    }

    override fun deserialize(decoder: Decoder): URL {
        TODO()
    }
}

fun box(): String {
    if (URLSerializer.descriptor.toString() != "PrimitiveDescriptor(java.net.URL)") return URLSerializer.descriptor.toString()
    return "OK"
}
