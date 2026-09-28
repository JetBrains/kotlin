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

// FILE: B.kt

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

object PublicSerializer : KSerializer<ViaPrivateAlias> {
    override val descriptor: SerialDescriptor get() = TODO()
    override fun serialize(encoder: Encoder, value: ViaPrivateAlias) {}
    override fun deserialize(decoder: Decoder): ViaPrivateAlias = TODO()
}

// The alias cannot be referenced from other files, even though the serializer object itself can.
private typealias PrivateAlias = PublicSerializer

<!CUSTOM_SERIALIZER_MAY_BE_INACCESSIBLE!>@Serializable(with = PrivateAlias::class)<!>
class ViaPrivateAlias

private object PrivateSerializer : KSerializer<ViaAliasOfPrivate> {
    override val descriptor: SerialDescriptor get() = TODO()
    override fun serialize(encoder: Encoder, value: ViaAliasOfPrivate) {}
    override fun deserialize(decoder: Decoder): ViaAliasOfPrivate = TODO()
}

// A public alias does not make the private serializer behind it reachable.
typealias <!EXPOSED_TYPEALIAS_EXPANDED_TYPE!>PublicAliasOfPrivate<!> = PrivateSerializer

<!CUSTOM_SERIALIZER_MAY_BE_INACCESSIBLE!>@Serializable(with = PublicAliasOfPrivate::class)<!>
class ViaAliasOfPrivate

object OkSerializer : KSerializer<Ok> {
    override val descriptor: SerialDescriptor get() = TODO()
    override fun serialize(encoder: Encoder, value: Ok) {}
    override fun deserialize(decoder: Decoder): Ok = TODO()
}

typealias OkAlias = OkSerializer

@Serializable(with = OkAlias::class)
class Ok

// Both the class and its serializer are private — nothing can leak.
private object PrivateForPrivate : KSerializer<PrivateBoth> {
    override val descriptor: SerialDescriptor get() = TODO()
    override fun serialize(encoder: Encoder, value: PrivateBoth) {}
    override fun deserialize(decoder: Decoder): PrivateBoth = TODO()
}

private typealias PrivateForPrivateAlias = PrivateForPrivate

@Serializable(with = PrivateForPrivateAlias::class)
private class PrivateBoth
