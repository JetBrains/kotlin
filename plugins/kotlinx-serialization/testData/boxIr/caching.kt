// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

import kotlinx.serialization.*

// Serializers for classes containing themselves as properties should be cached correctly
@Serializable
class Plain(val p: Plain? = null)

@Serializable
sealed class Sealed(val p: Sealed? = null)

@Serializable
open class Open(val p: Open? = null)

@Serializable
abstract class Abstract(val p: Abstract? = null)

@Serializable
class GenericHolder<T> {
    var value: T? = null
}

@Serializable
sealed class SealedGeneric {
    var holder = GenericHolder<SealedGeneric>()
}

@Serializable
sealed class SealedListGeneric {
    var holder = GenericHolder<GenericHolder<List<SealedListGeneric>>>()
}

@Serializable
class AccessFromCompanion(@Contextual val category: Any) {
    companion object {
        init {
            serializer().descriptor.getElementDescriptor(0).isNullable
        }
        var isNullable = serializer().descriptor.getElementDescriptor(0).isNullable
        val isNullable2 = serializer<AccessFromCompanion>().descriptor.getElementDescriptor(0).isNullable
    }
}


fun box(): String {
    // A correctly cached class must be initialized correctly in order to exclude cyclic nesting of caches
    Plain.serializer()
    Sealed.serializer()
    Open.serializer()
    Abstract.serializer()
    SealedGeneric.serializer()
    SealedListGeneric.serializer()
    AccessFromCompanion("any")

    return "OK"
}
