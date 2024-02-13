// FIR_IDENTICAL
// WITH_STDLIB
// FILE: test.kt
import kotlinx.serialization.*

open class NonSerializableParent(val arg: Int)

<!NON_SERIALIZABLE_PARENT_MUST_HAVE_NOARG_CTOR!>@Serializable<!>
class Derived(val someData: String): NonSerializableParent(42)
