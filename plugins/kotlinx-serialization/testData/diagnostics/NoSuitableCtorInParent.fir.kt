// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE
// WITH_STDLIB
// FILE: test.kt
import kotlinx.serialization.*

open class NonSerializableParent(val arg: Int)

@Serializable
class Derived(val someData: String): NonSerializableParent(42)