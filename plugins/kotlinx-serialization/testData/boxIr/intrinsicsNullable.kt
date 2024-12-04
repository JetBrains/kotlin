// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.modules.*
import java.lang.AssertionError

inline fun <reified T: Any?> listOfNullable(): KSerializer<List<T?>> = serializer<List<T?>>()
inline fun <reified T: Any> listOfNullableWithNonNullBound(): KSerializer<List<T?>> = serializer<List<T?>>()
inline fun <reified T> listOfNullableNoExplicitBound(): KSerializer<List<T?>> = serializer<List<T?>>()
inline fun <reified T> listOfNullableWithCast(): KSerializer<List<Any?>> = serializer<List<T?>>() as KSerializer<List<Any?>>
inline fun <reified T> listOfUnspecifiedNullability(): KSerializer<List<T>> = serializer<List<T>>()

inline fun <reified T> getSer(module: SerializersModule): KSerializer<T> {
    return module.serializer<T>()
}

fun check(shouldBeNullable: Boolean, descriptor: SerialDescriptor) {
    if (shouldBeNullable == descriptor.isNullable) return
    if (shouldBeNullable) throw java.lang.AssertionError("Should be nullable, but is not: $descriptor")
    throw java.lang.AssertionError("Should not be nullable, but it is: $descriptor")
}

fun box(): String {
    check(false, serializer<String>().descriptor)
    check(true, serializer<String?>().descriptor)

    check(false, serializer<List<String>>().descriptor.elementDescriptors.first())
    check(true, serializer<List<String?>>().descriptor.elementDescriptors.first())
    check(true, serializer<List<String>?>().descriptor)

    check(true, listOfNullable<String>().descriptor.elementDescriptors.first())
    check(true, listOfNullableNoExplicitBound<String>().descriptor.elementDescriptors.first())
    check(true, listOfNullableWithNonNullBound<String>().descriptor.elementDescriptors.first())
    check(true, listOfNullableWithCast<String>().descriptor.elementDescriptors.first())

    check(false, listOfUnspecifiedNullability<String>().descriptor.elementDescriptors.first())
    check(true, listOfUnspecifiedNullability<String?>().descriptor.elementDescriptors.first())

    val module = EmptySerializersModule()
    check(false, getSer<String>(module).descriptor)
    check(true, getSer<String?>(module).descriptor)

    return "OK"
}
