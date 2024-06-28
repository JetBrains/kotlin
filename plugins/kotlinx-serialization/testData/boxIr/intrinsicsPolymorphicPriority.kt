// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

// FILE: stub.kt

@file:JvmName("SerializersKt")

package kotlinx.serialization

import kotlin.reflect.KClass
import kotlinx.serialization.modules.*

// Copy of runtime function from kotlinx-serialization 1.7.0
fun moduleThenPolymorphic(module: SerializersModule, kClass: KClass<*>): KSerializer<*> {
    return module.getContextual(kClass) ?: PolymorphicSerializer(kClass)
}

fun moduleThenPolymorphic(module: SerializersModule, kClass: KClass<*>, argSerializers: Array<KSerializer<*>>): KSerializer<*> {
    return module.getContextual(kClass, argSerializers.asList()) ?: PolymorphicSerializer(kClass)
}

// FILE: test.kt

package a

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*
import kotlin.reflect.KClass
import kotlin.test.*

interface IApiError {
    val code: Int
}

object MyApiErrorSerializer : KSerializer<IApiError> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IApiError", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: IApiError) {
        TODO()
    }

    override fun deserialize(decoder: Decoder): IApiError {
        TODO()
    }
}

interface Parametrized<T> {
    val param: List<T>
}

class PSer<T>(val tSer: KSerializer<T>) : KSerializer<Parametrized<T>> {
    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor("PSer<${tSer.descriptor.serialName}>")

    override fun serialize(encoder: Encoder, value: Parametrized<T>) {
        TODO("Not yet implemented")
    }

    override fun deserialize(decoder: Decoder): Parametrized<T> {
        TODO("Not yet implemented")
    }
}

fun testParametrized() {
    val md = SerializersModule {
        contextual(Parametrized::class) { PSer(it[0]) }
    }
    assertEquals("PSer<kotlin.String>", md.serializer<Parametrized<String>>().descriptor.serialName)
}

fun box(): String {
    val module = serializersModuleOf(IApiError::class, MyApiErrorSerializer)
    assertSame(MyApiErrorSerializer, module.serializer<IApiError>() as KSerializer<IApiError>)
    assertEquals(
        MyApiErrorSerializer.descriptor,
        module.serializer<List<IApiError>>().descriptor.elementDescriptors.first()
    )
    testParametrized()
    return "OK"
}
