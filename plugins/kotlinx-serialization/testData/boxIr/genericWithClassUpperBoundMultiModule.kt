// WITH_STDLIB
// ISSUE: KT-89275

// MODULE: lib
// FILE: lib.kt

package a

import kotlinx.serialization.*

open class Base

@Serializable
class Leaf(val value: String) : Base()

@Serializable
class Box<T : Base>(val content: T)

// MODULE: main(lib)
// FILE: main.kt

import a.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

fun box(): String {
    val serial = Box.serializer(Leaf.serializer())
    val target = """{"content":{"value":"x"}}"""
    val decoded = Json.decodeFromString(serial, target)
    if (decoded.content.value != "x") return "Incorrect deserialization: ${decoded.content.value}"
    val encoded = Json.encodeToString(serial, decoded)
    if (encoded != target) return "Incorrect serialization: $encoded"
    return "OK"
}
