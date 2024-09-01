// TARGET_BACKEND: JVM_IR

// WITH_STDLIB
// FULL_JDK

@file:UseContextualSerialization(MyDate::class)

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*

typealias MyDate = java.time.LocalDate

/**
 * For K1 and BE, there is no difference if typealias or concrete type was used in the file-level @UseContextualSerialization.
 */
@Serializable
data class MyClass(val d: MyDate?, val dd: java.time.LocalDate)

fun box(): String {
    val descriptors = MyClass.serializer().descriptor.elementDescriptors.map { it.kind }
    return if (descriptors.all { it == SerialKind.CONTEXTUAL }) "OK" else descriptors.toString()
}
