// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@Serializable
data class Clashing(val fields: String) {
    companion object {
        <!SERIALIZER_FUNCTION_CLASH_IN_COMPANION!>fun serializer(): KSerializer<Clashing> = TODO()<!>
    }
}

@Serializable
class ClashingGeneric<T>(val t: T) {
    companion object {
        <!SERIALIZER_FUNCTION_CLASH_IN_COMPANION!>fun <E> serializer(ser: KSerializer<E>): KSerializer<ClashingGeneric<E>> = TODO()<!>
    }
}

// Different arity than the class' type parameter count — the plugin still finds its own function.
@Serializable
data class NotClashingByArity(val fields: String) {
    companion object {
        fun serializer(unused: KSerializer<String>): KSerializer<NotClashingByArity> = TODO()
    }
}

// Non-serializable class, the plugin generates nothing for the companion.
class NotSerializable {
    companion object {
        fun serializer(): KSerializer<NotSerializable> = TODO()
    }
}

object KeptSerializer : KSerializer<WithKeptSerializer> {
    override val descriptor: SerialDescriptor get() = TODO()
    override fun serialize(encoder: Encoder, value: WithKeptSerializer) {}
    override fun deserialize(decoder: Decoder): WithKeptSerializer = TODO()
}

@Serializable(KeptSerializer::class)
@KeepGeneratedSerializer
data class WithKeptSerializer(val fields: String) {
    companion object {
        <!SERIALIZER_FUNCTION_CLASH_IN_COMPANION!>fun generatedSerializer(): KSerializer<WithKeptSerializer> = TODO()<!>
    }
}

// @KeepGeneratedSerializer is not applied, so `generatedSerializer()` is not generated — no clash.
@Serializable
data class NoKeptSerializer(val fields: String) {
    companion object {
        fun generatedSerializer(): KSerializer<NoKeptSerializer> = TODO()
    }
}

// For a serializable object the plugin generates `serializer()` in the object itself, not in a companion.
@Serializable
object ClashingObject {
    <!SERIALIZER_FUNCTION_CLASH_IN_COMPANION!>fun serializer(): KSerializer<ClashingObject> = TODO()<!>
}

@Serializable
object NotClashingObject {
    fun serializer(unused: KSerializer<String>): KSerializer<NotClashingObject> = TODO()
}

// The backend counts every parameter but the dispatch receiver, so a receiver makes the signature not match
// and the generated function is still found.
class Receiver

@Serializable
data class NotClashingByReceiver(val fields: String) {
    companion object {
        fun Receiver.serializer(): KSerializer<NotClashingByReceiver> = TODO()
    }
}

// The return type is compared after expansion, just like in the backend.
typealias SerializerAlias<T> = KSerializer<T>

@Serializable
data class ClashingViaTypealias(val fields: String) {
    companion object {
        <!SERIALIZER_FUNCTION_CLASH_IN_COMPANION!>fun serializer(): SerializerAlias<ClashingViaTypealias> = TODO()<!>
    }
}
