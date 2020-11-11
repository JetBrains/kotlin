// CURIOUS_ABOUT test
// WITH_RUNTIME

import kotlinx.serialization.*

@Serializable
class Simple(val firstName: String, val lastName: String)

@Serializable
data class Box<out T>(val boxed: T)

@Serializable
object SerializableObject {}

inline fun <reified T: Any> getSer(): KSerializer<T> {
    return serializer<T>()
}

inline fun <reified T: Any> getBoxSer(): KSerializer<Box<T>> {
    return serializer<Box<T>>()
}

inline fun <reified T: Any> listSer(): KSerializer<List<T>> {
    return serializer<List<T>>()
}

fun test() {
    serializer<Simple>()
    getSer<Simple>()
    getSer<Box<Simple>>()
    getBoxSer<Simple>()
    listSer<Simple>()

    serializer<Box<List<Simple>>>()

    listSer<Box<List<Simple>>>()

    serializer<Int>()

    serializer<SerializableObject>()

    listSer<List<Box<Int>>>()
}