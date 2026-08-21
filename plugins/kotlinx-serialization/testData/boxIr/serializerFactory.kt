// WITH_STDLIB
// ISSUE: KT-58501

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable object Objekt

@Serializable sealed class SealedInterface
@Serializable data object Inheritor: SealedInterface()

@Serializable enum class EnumKlass { INSTANCE }

@Serializable class Plain

fun box(): String {
    serializer<Objekt>()
    Objekt.serializer()
    serializer<EnumKlass>()
    EnumKlass.serializer()
    serializer<SealedInterface>()
    SealedInterface.serializer()
    serializer<Plain>()
    Plain.serializer()
    return "OK"
}
