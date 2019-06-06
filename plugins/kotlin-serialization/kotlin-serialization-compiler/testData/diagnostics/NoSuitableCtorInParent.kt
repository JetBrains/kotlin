// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE

// FILE: test.kt
import kotlinx.serialization.*

open class NonSerializableParent(val arg: Int)

<!PLUGIN_ERROR("Impossible to make this class serializable because its parent is not serializable and does not have exactly one constructor without parameters")!>@Serializable<!>
class Derived(val someData: String): NonSerializableParent(42)
