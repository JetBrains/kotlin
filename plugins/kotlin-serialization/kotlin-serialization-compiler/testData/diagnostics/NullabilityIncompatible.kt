// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE

// FILE: test.kt
import kotlinx.serialization.*

@Serializable(NopeNullableSerializer::class)
class Nope {}

class NopeNullableSerializer: KSerializer<Nope?> {
    override val descriptor: SerialDescriptor get() = TODO()
    override fun deserialize(decoder: Decoder): Nope? = TODO()
    override fun serialize(encoder: Encoder, obj: Nope?) = TODO()
}

@Serializable
class Foo(val foo: <!SERIALIZER_NULLABILITY_INCOMPATIBLE("NopeNullableSerializer", "Nope")!>Nope<!>)