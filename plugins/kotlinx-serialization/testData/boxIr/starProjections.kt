// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*

interface E

@Serializable
class Box<T: E>(val boxed: T)

@Serializable
class Wrapper(val boxed: Box<*>)

fun box(): String {
    val s = Wrapper.serializer().descriptor.elementDescriptors.joinToString()
    return if (s == "Box(boxed: kotlinx.serialization.Polymorphic<E>)") "OK" else s
}
