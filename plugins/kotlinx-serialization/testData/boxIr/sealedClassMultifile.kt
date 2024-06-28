// WITH_STDLIB

// FILE: a.kt

import kotlinx.serialization.*

@Serializable
sealed class Base {
    abstract val id: Long
}

// FILE: b.kt

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*

@Serializable
class DerivedOtherFile<Value>(
    override val id: Long,
    val value: Value
) : Base()

fun box(): String {
    val desc = Base.serializer().descriptor.kind
    return if (desc == PolymorphicKind.SEALED) "OK" else desc.toString()
}
