// WITH_STDLIB

import kotlinx.serialization.*

@Serializable
data class WithPrivateCompanion(val myString: String = DEFAULT) {
    <!PRIVATE_COMPANION_OF_SERIALIZABLE!>private<!> companion object {
        const val DEFAULT = "private_default"
    }
}

@Serializable
data class WithNamedPrivateCompanion(val myString: String) {
    <!PRIVATE_COMPANION_OF_SERIALIZABLE!>private<!> companion object Named
}

// The serializer of a non-public class is not part of the module API anyway.
@Serializable
private data class PrivateClass(val myString: String) {
    private companion object
}

@Serializable
internal data class InternalClass(val myString: String) {
    private companion object
}

@Serializable
data class WithInternalCompanion(val myString: String) {
    internal companion object
}

@Serializable
data class WithPublicCompanion(val myString: String) {
    companion object
}

@Serializable
sealed interface SealedWithPrivateCompanion {
    <!PRIVATE_COMPANION_OF_SERIALIZABLE!>private<!> companion object
}

// Not serializable — nothing is generated in the companion.
data class NotSerializable(val myString: String) {
    private companion object
}
