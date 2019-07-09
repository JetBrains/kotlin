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
class Foo(val foo: <!PLUGIN_ERROR("Type 'Nope' is non-nullable and therefore can not be serialized with serializer for nullable type 'NopeNullableSerializer'")!>Nope<!>)