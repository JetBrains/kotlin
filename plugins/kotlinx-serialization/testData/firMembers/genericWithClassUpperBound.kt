// WITH_STDLIB
// DUMP_IR
// ISSUE: KT-89275

import kotlinx.serialization.*
import kotlinx.serialization.json.*

open class Base

@Serializable
class Leaf(val value: String) : Base()

@Serializable
class Box<T : Base>(val content: T)

fun box(): String {
    val serial = Box.serializer(Leaf.serializer())
    val target = """{"content":{"value":"x"}}"""
    val decoded = Json.decodeFromString(serial, target)
    if (decoded.content.value != "x") return "Incorrect deserialization: ${decoded.content.value}"
    val encoded = Json.encodeToString(serial, decoded)
    if (encoded != target) return "Incorrect serialization: $encoded"
    return "OK"
}
