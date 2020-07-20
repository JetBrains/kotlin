// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE,-PROVIDED_RUNTIME_TOO_LOW

// FILE: test.kt
import kotlinx.serialization.*

@Serializable
open class Parent(open val arg: Int)

<!DUPLICATE_SERIAL_NAME("arg")!>@Serializable<!>
class Derived(override val arg: Int): Parent(arg)
