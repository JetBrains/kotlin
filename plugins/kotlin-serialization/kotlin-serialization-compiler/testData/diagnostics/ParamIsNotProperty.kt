// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE

// FILE: test.kt
import kotlinx.serialization.*

<!PLUGIN_ERROR("This class is not serializable automatically because it has primary constructor parameters of which are not properties")!>@Serializable<!>
class Test(val someData: String, cantBeDeserialized: Int)
