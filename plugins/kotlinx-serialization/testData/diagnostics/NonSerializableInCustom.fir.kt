// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

class NonSerializable

class Box<T>

class BoxSerializer<T>(val element: KSerializer<T>): KSerializer<Box<T>> {
    override val descriptor: SerialDescriptor
        get() = TODO()

    override fun serialize(encoder: Encoder, value: Box<T>) = TODO()

    override fun deserialize(decoder: Decoder): Box<T> = TODO()
}

class BoxSerializerArgless(): KSerializer<Box<Any>> {
    override val descriptor: SerialDescriptor
        get() = TODO()

    override fun serialize(encoder: Encoder, value: Box<Any>) = TODO()

    override fun deserialize(decoder: Decoder): Box<Any> = TODO()
}

@Serializable
class TestCase(@Serializable(BoxSerializer::class) val box: Box<NonSerializable>)

@Serializable
class TestCase2(@Serializable(BoxSerializerArgless::class) val box: Box<NonSerializable>)

@Serializable
class TestCase3(@Serializable(BoxSerializer::class) val box: Box<List<NonSerializable>>)
