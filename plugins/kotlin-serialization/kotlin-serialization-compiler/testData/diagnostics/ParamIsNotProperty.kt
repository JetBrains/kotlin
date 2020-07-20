// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE,-PROVIDED_RUNTIME_TOO_LOW

// FILE: test.kt
import kotlinx.serialization.*

<!PRIMARY_CONSTRUCTOR_PARAMETER_IS_NOT_A_PROPERTY!>@Serializable<!>
class Test(val someData: String, cantBeDeserialized: Int)
