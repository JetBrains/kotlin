// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.modules.*

@Serializable
class Simple(val firstName: String, val lastName: String)

@Serializable
data class Box<out T>(val boxed: T)

@Serializable
object SerializableObject {}

val module = SerializersModule {}

inline fun <reified T : Any> getSer(module: SerializersModule): KSerializer<T> {
    return module.serializer<T>()
}

inline fun <reified T : Any> getBoxSer(module: SerializersModule): KSerializer<Box<T>> {
    return module.serializer<Box<T>>()
}

inline fun <reified T : Any> listSer(module: SerializersModule): KSerializer<List<T>> {
    return module.serializer<List<T>>()
}

fun SerialDescriptor.recursiveToString(): String {
    return (0 until elementsCount).joinToString(", ", "$serialName(", ")") { i ->
        getElementName(i) + ": " + getElementDescriptor(i).recursiveToString()
    }
}

fun assertHasSerializers(kSerializer: KSerializer<*>, list: List<String>) {
    var str = kSerializer.descriptor.recursiveToString()
    for (name in list) {
        if (name !in str) error("Not found $name in $str")
        str = str.replaceBefore(name, "")
        str = str.removePrefix(name)
    }
}

fun box(): String {
    assertHasSerializers(serializer<Simple>(), listOf("Simple"))
    assertHasSerializers(getSer<Simple>(module), listOf("Simple"))

    assertHasSerializers(getSer<Box<Simple>>(module), listOf("Box", "Simple"))
    assertHasSerializers(getBoxSer<Simple>(module), listOf("Box", "Simple"))

    assertHasSerializers(listSer<Simple>(module), listOf("ArrayList", "Simple"))
    assertHasSerializers(serializer<Box<List<Simple>>>(), listOf("Box", "ArrayList", "Simple"))
    assertHasSerializers(listSer<Box<List<Simple>>>(module), listOf("ArrayList", "Box", "ArrayList", "Simple"))

    assertHasSerializers(serializer<Int>(), listOf("Int"))
    assertHasSerializers(serializer<SerializableObject>(), listOf("SerializableObject"))
    assertHasSerializers(listSer<List<Box<Int>>>(module), listOf("ArrayList", "ArrayList", "Box", "Int"))
    return "OK"
}