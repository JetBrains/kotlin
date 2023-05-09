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


fun box(): String {
    // A correctly cached class must be initialized correctly in order to exclude cyclic nesting of caches
    Plain.serializer()
    Sealed.serializer()
    Open.serializer()
    Abstract.serializer()

    return "OK"
}
