// WITH_STDLIB

package a

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlin.reflect.*


object InterfaceCustomSerializer: ToDoSerializer<InterfaceWithCustom>("interface")

@Serializable(InterfaceCustomSerializer::class)
interface InterfaceWithCustom


object NamedInterfaceSerializer: ToDoSerializer<NamedInterfaceWithCustom>("interface.named")

@Serializable(NamedInterfaceSerializer::class)
interface NamedInterfaceWithCustom {
    companion object Named {

    }
}


@Serializable
sealed interface SealedInterface

@Serializable
sealed interface NamedSealedInterface {
    companion object Named {

    }
}

object SealedInterfaceCustomSerializer: ToDoSerializer<SealedInterfaceWithCustom>("sealed interface")

@Serializable(SealedInterfaceCustomSerializer::class)
sealed interface SealedInterfaceWithCustom

object NamedSealedInterfaceCustomSerializer: ToDoSerializer<NamedSealedInterfaceWithCustom>("sealed interface.named")

@Serializable(NamedSealedInterfaceCustomSerializer::class)
sealed interface NamedSealedInterfaceWithCustom {
    companion object Named {

    }
}




@Serializable
class Holder(
    val a: InterfaceWithCustom,
    val b: NamedInterfaceWithCustom,
    val c: SealedInterface,
    val d: NamedSealedInterface,
    val e: SealedInterfaceWithCustom,
    val f: NamedSealedInterfaceWithCustom,
)

fun box(): String {
    check(InterfaceWithCustom.serializer(), "interface", 0)?.let { return it }
    check(NamedInterfaceWithCustom.Named.serializer(), "interface.named", 1, true)?.let { return it }

    check(SealedInterface.serializer(), "a.SealedInterface", 2)?.let { return it }
    check(NamedSealedInterface.serializer(), "a.NamedSealedInterface", 3, true)?.let { return it }

    check(SealedInterfaceWithCustom.serializer(), "sealed interface", 4)?.let { return it }
    check(NamedSealedInterfaceWithCustom.serializer(), "sealed interface.named", 5, true)?.let { return it }

    return "OK"
}

inline fun <reified T> check(serializer: KSerializer<T>, serializerName: String, index: Int, namedCompanion: Boolean = false): String? {
    if (serializer.descriptor.serialName != serializerName) return "${T::class.simpleName}.serializer().descriptor.serialName=" + serializer.descriptor.serialName
    if (serializer<T>().descriptor.serialName != serializerName) return "serializer<${T::class.simpleName}>().descriptor.serialName=" + serializer<T>().descriptor.serialName
    if (!namedCompanion && serializer(typeOf<T>()).descriptor.serialName != serializerName) return "serializer(typeOf<${T::class.simpleName}>()).descriptor.serialName=" + serializer(typeOf<T>()).descriptor.serialName
    if (Holder.serializer().descriptor.getElementDescriptor(index).serialName != serializerName) return "holderDescriptor.getElementDescriptor($index).serialName=" + Holder.serializer().descriptor.getElementDescriptor(index).serialName

    return null
}

abstract class ToDoSerializer<T: Any>(descriptorName: String): KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(descriptorName, PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): T = TODO()
    override fun serialize(encoder: Encoder, value: T) = TODO()
}