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
