// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE
// WITH_STDLIB
// FILE: test.kt
import kotlinx.serialization.*

@Serializable
open class Parent(open val arg: Int)

@Serializable
class Derived(override val arg: Int): Parent(arg)