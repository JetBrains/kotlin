// WITH_STDLIB
// FILE: test.kt
import kotlinx.serialization.*

@Serializable
class Test(val someData: String, cantBeDeserialized: Int)
