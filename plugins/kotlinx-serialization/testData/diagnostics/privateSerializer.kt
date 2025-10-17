// FIR_IDENTICAL
// WITH_STDLIB

// FILE: A.kt

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

<!CUSTOM_SERIALIZER_MAY_BE_INACCESSIBLE!>@Serializable(with = C::class)<!>
interface A

private object C : KSerializer<A> {
    override val descriptor: SerialDescriptor get() = TODO()
    override fun serialize(encoder: Encoder, value: A) {}
    override fun deserialize(decoder: Decoder): A = TODO()
}
