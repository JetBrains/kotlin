// FIR_IDENTICAL
// SKIP_TXT

// FILE: test.kt
import kotlinx.serialization.*

class Foo(i: Int, val j: Int)

<!EXTERNAL_CLASS_NOT_SERIALIZABLE!>@Serializer(forClass = Foo::class)<!>
object ExternalSerializer
