// WITH_STDLIB

package a

import kotlinx.serialization.*

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlin.test.assertEquals

class Dummy

class DummyBox<T>

@Serializable(ClassSerializerOnClass::class)
class DummySpecified

class ClassSerializerGeneric : KSerializer<DummyBox<String>> {
    override val descriptor get() = PrimitiveSerialDescriptor("ClassSerializerGeneric", PrimitiveKind.INT)
    override fun deserialize(decoder: Decoder): DummyBox<String> = TODO()
    override fun serialize(encoder: Encoder, value:DummyBox<String>): Unit = TODO()
}

class ClassSerializerDummy : KSerializer<Dummy> {
    override val descriptor get() = PrimitiveSerialDescriptor("ClassSerializerDummy", PrimitiveKind.INT)
    override fun deserialize(decoder: Decoder): Dummy = TODO()
    override fun serialize(encoder: Encoder, value: Dummy): Unit = TODO()
}

object ObjectSerializerGeneric: KSerializer<DummyBox<String>> {
    override val descriptor get() = PrimitiveSerialDescriptor("ObjectSerializerGeneric", PrimitiveKind.INT)
    override fun deserialize(decoder: Decoder): DummyBox<String> = TODO()
    override fun serialize(encoder: Encoder, value: DummyBox<String>): Unit = TODO()
}

object ObjectSerializerDummy: KSerializer<Dummy> {
    override val descriptor get() = PrimitiveSerialDescriptor("ObjectSerializerDummy", PrimitiveKind.INT)
    override fun deserialize(decoder: Decoder): Dummy = TODO()
    override fun serialize(encoder: Encoder, value:Dummy): Unit = TODO()
}

class ClassSerializerOnClass: KSerializer<DummySpecified> {
    override val descriptor get() = PrimitiveSerialDescriptor("ClassSerializerOnClass", PrimitiveKind.INT)
    override fun deserialize(decoder: Decoder): DummySpecified = TODO()
    override fun serialize(encoder: Encoder, value:DummySpecified): Unit = TODO()
}

@Serializable
class Holder(
    @Serializable(ClassSerializerGeneric::class) val a: DummyBox<String>,
    @Serializable(ClassSerializerDummy::class) val b: Dummy,
    @Serializable(ObjectSerializerGeneric::class) val c: DummyBox<String>,
    @Serializable(ObjectSerializerDummy::class) val d: Dummy,
    val e: DummySpecified
)

fun box(): String {
    val descs = Holder.serializer().descriptor.elementDescriptors.toList()
    assertEquals("ClassSerializerGeneric", descs[0].serialName)
    assertEquals("ClassSerializerDummy", descs[1].serialName)
    assertEquals("ObjectSerializerGeneric", descs[2].serialName)
    assertEquals("ObjectSerializerDummy", descs[3].serialName)
    assertEquals("ClassSerializerOnClass", descs[4].serialName)
    return "OK"
}
