// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class SomeClass(val i: Int)

fun box(): String {
    val targetString = """{"i":42}"""
    val serializer = SomeClass.Companion.serializer()
    val descriptor = serializer.descriptor
    if (descriptor.toString() != "SomeClass(i: kotlin.Int)") return "Incorrect SerialDescriptor.toString(): $descriptor"
    val instance = SomeClass(42)
    val string = Json.encodeToString(serializer, instance)
    if (string != targetString) return "Incorrect serialization result: expected $targetString, got $string"
    val instance2 = Json.decodeFromString(serializer, string)
    if (instance2.i != instance.i) return "Incorrect deserialization result: expected ${instance.i}, got ${instance2.i}"
    return "OK"
}


