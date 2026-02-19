// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*

interface SomeInterface

@Serializable
sealed class SealedMultiple<T1 : SomeInterface, T2 : SomeInterface>

@Serializable
abstract class AbstractMultiple<T1 : SomeInterface, T2 : SomeInterface>

@Serializable
sealed interface SealedInterfaceMultiple<T1 : SomeInterface, T2 : SomeInterface>

interface InterfaceMultiple<T1 : SomeInterface, T2 : SomeInterface>

@Serializable
class ToSerialize(
    val a: SealedMultiple<SomeInterface, SomeInterface>,
    val b: AbstractMultiple<*, *>,
    val c: SealedInterfaceMultiple<*, *>,
    val d: InterfaceMultiple<*, *>
)

fun box(): String {
    val s = ToSerialize.serializer().descriptor.elementDescriptors.joinToString { it.kind.toString() }
    return if (s == "SEALED, OPEN, SEALED, OPEN") "OK" else s
}
