// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE, -OPT_IN_USAGE
// SKIP_TXT

// FILE: test.kt
import kotlinx.serialization.*

class Foo(i: Int, val j: Int)

@Serializer(forClass = Foo::class)
object ExternalSerializer