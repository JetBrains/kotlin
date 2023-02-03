// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.modules.*
import kotlin.test.*

@Serializable
@JsonClassDiscriminator("sealed")
sealed class Sealed

@Serializable
@JsonClassDiscriminator("abstract")
abstract class Abstract

@Serializable
@JsonClassDiscriminator("object")
object Object

fun test(descriptor: SerialDescriptor, value: String, name: String = descriptor.serialName) {
    val anno = descriptor.annotations.filterIsInstance<JsonClassDiscriminator>().singleOrNull()
    requireNotNull(anno) { "Annotation value not found for $name" }
    assertEquals(anno.discriminator, value, "Annotation value incorrect for $name")
}

fun box(): String {
    test(serializer<Sealed>().descriptor, "sealed")
    test(serializer<Abstract>().descriptor, "abstract")
    test(serializer<Object>().descriptor, "object")
    test(serializer<List<Sealed>>().descriptor.getElementDescriptor(0), "sealed", "List<Sealed>")
    test(serializer<List<Abstract>>().descriptor.getElementDescriptor(0), "abstract", "List<Abstract>")
    test(serializer<List<Object>>().descriptor.getElementDescriptor(0), "object", "List<Object>")
    test(serializer<Array<Sealed>>().descriptor.getElementDescriptor(0), "sealed", "Array<Sealed>")
    return "OK"
}
