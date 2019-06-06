// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE

// FILE: test.kt
import kotlinx.serialization.*

@Serializable
open class Parent(open val arg: Int)

<!PLUGIN_ERROR("Serializable class has duplicate serial name of property 'arg', either in the class itself or its supertypes")!>@Serializable<!>
class Derived(override val arg: Int): Parent(arg)
