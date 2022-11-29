// CURIOUS_ABOUT: test, getSer
// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

@Serializable
class Simple(val firstName: String, val lastName: String)

class NoSer

class NoSerGeneric<T>

val module = SerializersModule {}

inline fun <reified T: Any> getSer(module: SerializersModule): KSerializer<T> {
    return module.serializer()
}

fun test() {
    module.serializer<Simple>()
    module.serializer<NoSer>()
    module.serializer<List<Simple>>()
    module.serializer<List<NoSer>>()

    getSer<Simple>(module)
    getSer<NoSer>(module)

    getSer<NoSerGeneric<Simple>>(module)
    getSer<NoSerGeneric<NoSer>>(module)
}