// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.descriptors.*

@Serializable
class Simple(val firstName: String, val lastName: String)

@Serializable
data class Box<out T>(val boxed: T)

@Serializable
object SerializableObject {}

inline fun <reified T : Any> getSer(): KSerializer<T> {
    return serializer<T>()
}

inline fun <reified T : Any> getBoxSer(): KSerializer<Box<T>> {
    return serializer<Box<T>>()
}

inline fun <reified T : Any> listSer(): KSerializer<List<T>> {
    return serializer<List<T>>()
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
    assertHasSerializers(getSer<Simple>(), listOf("Simple"))

    assertHasSerializers(getSer<Box<Simple>>(), listOf("Box", "Simple"))
    assertHasSerializers(getBoxSer<Simple>(), listOf("Box", "Simple"))

    assertHasSerializers(listSer<Simple>(), listOf("ArrayList", "Simple"))
    assertHasSerializers(serializer<Box<List<Simple>>>(), listOf("Box", "ArrayList", "Simple"))
    assertHasSerializers(listSer<Box<List<Simple>>>(), listOf("ArrayList", "Box", "ArrayList", "Simple"))

    assertHasSerializers(serializer<Int>(), listOf("Int"))
    assertHasSerializers(serializer<SerializableObject>(), listOf("SerializableObject"))
    assertHasSerializers(listSer<List<Box<Int>>>(), listOf("ArrayList", "ArrayList", "Box", "Int"))
    return "OK"
}