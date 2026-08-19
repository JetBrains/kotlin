// WITH_STDLIB
// ISSUE: KT-59768

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable sealed class SealedInterface {
    companion object {
        fun unrelated() {}
    }
}
@Serializable enum class EnumKlass { INSTANCE;
    companion object  {
        fun unrelated() {}
    }
}

@Serializable class Plain {
    companion object  {
        fun unrelated() {}
    }
}

@MetaSerializable
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class MySerializable

@MySerializable
sealed interface SealedMeta {
    companion object  {
        fun unrelated() {}
    }
}

fun box(): String {
    serializer<EnumKlass>()
    EnumKlass.serializer()
    serializer<SealedInterface>()
    SealedInterface.serializer()
    serializer<Plain>()
    Plain.serializer()
    serializer<SealedMeta>()
    SealedMeta.serializer()
    return "OK"
}
