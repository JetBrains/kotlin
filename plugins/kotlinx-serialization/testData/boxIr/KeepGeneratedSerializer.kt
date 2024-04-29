// WITH_STDLIB
// FIR_IDENTICAL

// FILE: annotation.kt
package kotlinx.serialization

import kotlin.annotation.*

/*
  Until the annotation is added to the serialization runtime,
  we have to create an annotation with that name in the project itself
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class KeepGeneratedSerializer

// FILE: main.kt
@file:Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE") // TODO: support common sources in the test infrastructure

import kotlin.jvm.*
import kotlin.test.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*

// == value class
@Serializable(with = ValueSerializer::class)
@KeepGeneratedSerializer
@JvmInline
value class Value(val i: Int)

object ValueSerializer: KSerializer<Value> {
    override val descriptor = PrimitiveSerialDescriptor("ValueSerializer", PrimitiveKind.INT)
    override fun deserialize(decoder: Decoder): Value {
        val value = decoder.decodeInt()
        return Value(value - 42)
    }
    override fun serialize(encoder: Encoder, value: Value) {
        encoder.encodeInt(value.i + 42)
    }
}

// == final class ==
@Serializable(with = DataSerializer::class)
@KeepGeneratedSerializer
data class Data(val i: Int)

object DataSerializer: KSerializer<Data> {
    override val descriptor = PrimitiveSerialDescriptor("DataSerializer", PrimitiveKind.INT)
    override fun deserialize(decoder: Decoder): Data {
        val value = decoder.decodeInt()
        return Data(value)
    }
    override fun serialize(encoder: Encoder, value: Data) {
        encoder.encodeInt(value.i)
    }
}

// == inheritance ==
@Serializable(with = ParentSerializer::class)
@KeepGeneratedSerializer
open class Parent(val p: Int) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Parent) return false

        if (p != other.p) return false

        return true
    }

    override fun hashCode(): Int {
        return p
    }
}

object ParentSerializer: KSerializer<Parent> {
    override val descriptor = PrimitiveSerialDescriptor("ParentSerializer", PrimitiveKind.INT)
    override fun deserialize(decoder: Decoder): Parent {
        val value = decoder.decodeInt()
        return Parent(value - 1)
    }
    override fun serialize(encoder: Encoder, value: Parent) {
        encoder.encodeInt(value.p + 1)
    }
}

@Serializable
data class Child(val c: Int): Parent(0)

@Serializable(with = ChildSerializer::class)
@KeepGeneratedSerializer
data class ChildWithCustom(val c: Int): Parent(0)

object ChildSerializer: KSerializer<ChildWithCustom> {
    override val descriptor = PrimitiveSerialDescriptor("ChildSerializer", PrimitiveKind.INT)
    override fun deserialize(decoder: Decoder): ChildWithCustom {
        val value = decoder.decodeInt()
        return ChildWithCustom(value - 2)
    }

    override fun serialize(encoder: Encoder, value: ChildWithCustom) {
        encoder.encodeInt(value.c + 2)
    }
}

// == enums ==
@Serializable(with = MyEnumSerializer::class)
@KeepGeneratedSerializer
enum class MyEnum {
    A,
    B,
    FALLBACK
}

@Serializable
data class EnumHolder(val e: MyEnum)

object MyEnumSerializer: KSerializer<MyEnum> {
    val defaultSerializer = MyEnum.generatedSerializer()

    override val descriptor = PrimitiveSerialDescriptor("MyEnumSerializer", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): MyEnum {
        decoder.decodeString()
        return MyEnum.A
    }

    override fun serialize(encoder: Encoder, value: MyEnum) {
        // always encode FALLBACK entry by generated serializer
        defaultSerializer.serialize(encoder, MyEnum.FALLBACK)
    }
}

// == parametrized ==
@Serializable(with = ParametrizedSerializer::class)
@KeepGeneratedSerializer
data class ParametrizedData<T>(val t: T)

class ParametrizedSerializer(val serializer: KSerializer<Any>): KSerializer<ParametrizedData<Any>> {
    override val descriptor = PrimitiveSerialDescriptor("ParametrizedSerializer", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): ParametrizedData<Any> {
        val value = serializer.deserialize(decoder)
        return ParametrizedData(value)
    }

    override fun serialize(encoder: Encoder, value: ParametrizedData<Any>) {
        serializer.serialize(encoder, value.t)
    }
}

// == companion external serializer ==
@Serializable(WithCompanion.Companion::class)
@KeepGeneratedSerializer
data class WithCompanion(val value: Int) {
    @Serializer(WithCompanion::class)
    companion object {
        override val descriptor = PrimitiveSerialDescriptor("WithCompanionDesc", PrimitiveKind.INT)
        override fun deserialize(decoder: Decoder): WithCompanion {
            val value = decoder.decodeInt()
            return WithCompanion(value)
        }

        override fun serialize(encoder: Encoder, value: WithCompanion) {
            encoder.encodeInt(value.value)
        }
    }
}

// == serializable object ==
@Serializable(with = ObjectSerializer::class)
@KeepGeneratedSerializer
object Object

object ObjectSerializer: KSerializer<Object> {
    override val descriptor = PrimitiveSerialDescriptor("ObjectSerializer", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Object {
        decoder.decodeInt()
        return Object
    }
    override fun serialize(encoder: Encoder, value: Object) {
        encoder.encodeInt(8)
    }
}

fun box(): String = boxWrapper {
    val value = Value(42)
    val data = Data(42)
    val child = Child(1)
    val param = ParametrizedData<Data>(data)

    test(Value(1), "43", "1", Value.serializer(), Value.generatedSerializer())
    test(Data(2), "2", "{\"i\":2}", Data.serializer(), Data.generatedSerializer())
    test(Parent(3), "4", "{\"p\":3}", Parent.serializer(), Parent.generatedSerializer())
    test(Child(4), "{\"p\":0,\"c\":4}", "", Child.serializer(), null)
    test(ChildWithCustom(5), "7", "{\"p\":0,\"c\":5}", ChildWithCustom.serializer(), ChildWithCustom.generatedSerializer())
    test(ParametrizedData<Data>(Data(6)), "6", "{\"t\":6}", ParametrizedData.serializer(Data.serializer()), ParametrizedData.generatedSerializer(Data.serializer()))
    test(WithCompanion(7), "7", "{\"value\":7}", WithCompanion.serializer(), WithCompanion.generatedSerializer())
    test(Object, "8", "{}", Object.serializer(), Object.generatedSerializer())

    test(MyEnum.A, "\"FALLBACK\"", "\"A\"", MyEnum.serializer(), MyEnum.generatedSerializer())
    assertTrue(serializer<MyEnum>() is MyEnumSerializer, "serializer<MyEnum> illegal = " + serializer<MyEnum>())
    assertTrue(MyEnum.serializer() is MyEnumSerializer, "MyEnum.serializer() illegal = " + MyEnum.serializer())
    assertEquals("kotlinx.serialization.internal.EnumSerializer<MyEnum>", MyEnum.generatedSerializer().toString(), "MyEnum.generatedSerializer() illegal")
    assertSame(MyEnum.generatedSerializer(), MyEnum.generatedSerializer(), "MyEnum.generatedSerializer() instance differs")

    assertEquals("Object()", Object.generatedSerializer().descriptor.toString(), "Object.generatedSerializer() illegal")
    assertSame(Object.generatedSerializer(), Object.generatedSerializer(), "Object.generatedSerializer() instance differs")
}

inline fun <reified T : Any> test(
    value: T,
    customJson: String,
    keepJson: String,
    serializer: KSerializer<T>,
    generatedSerializer: KSerializer<T>?
) {
    val implicitJson = Json.encodeToString(value)
    assertEquals(customJson, implicitJson, "Json.encodeToString(value: ${T::class.simpleName})")
    val implicitDecoded = Json.decodeFromString<T>(implicitJson)
    assertEquals(value, implicitDecoded, "Json.decodeFromString(json): ${T::class.simpleName}")

    val exlicitJson = Json.encodeToString(serializer, value)
    assertEquals(customJson, exlicitJson, "Json.encodeToString(${T::class.simpleName}.serializer(), value)")
    val explicitDecoded = Json.decodeFromString(serializer, exlicitJson)
    assertEquals(value, explicitDecoded, "Json.decodeFromString(${T::class.simpleName}.serializer(), json)")

    if (generatedSerializer == null) return
    val keep = Json.encodeToString(generatedSerializer, value)
    assertEquals(keepJson, keep, "Json.encodeToString(${T::class.simpleName}.generatedSerializer(), value)")
    val keepDecoded = Json.decodeFromString(generatedSerializer, keep)
    assertEquals(value, keepDecoded, "Json.decodeFromString(${T::class.simpleName}.generatedSerializer(), json)")
}

fun boxWrapper(block: () -> Unit): String {
    var result = "OK"
    try {
        block()
    } catch (e: Exception) {
        result = e.message ?: e.toString()
    }
    return result
}

