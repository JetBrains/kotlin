// FIR_IDENTICAL
// WITH_STDLIB

import kotlinx.serialization.*

// Typealias for Serializable annotation
typealias MySerializable = Serializable

// Normal usage of Serializable (no warning)
@Serializable
class NormalClass(val value: String)

// Usage of typealiased Serializable (should trigger warning)
@MySerializable
class TypeAliasedClass(val value: String)

// Usage of typealiased Serializable with custom serializer (should trigger warning)
@MySerializable(CustomSerializer::class)
class TypeAliasedClassWithCustomSerializer(val value: String)

// Custom serializer
object CustomSerializer : KSerializer<TypeAliasedClassWithCustomSerializer> {
    override val descriptor = serialDescriptor<String>()
    
    override fun serialize(encoder: Encoder, value: TypeAliasedClassWithCustomSerializer) {
        encoder.encodeString(value.value)
    }
    
    override fun deserialize(decoder: Decoder): TypeAliasedClassWithCustomSerializer {
        return TypeAliasedClassWithCustomSerializer(decoder.decodeString())
    }
}