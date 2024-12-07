// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
object FooBar

fun box(): String {
    val encoded = Json.encodeToString(FooBar.serializer(), FooBar)
    if (encoded != "{}") return encoded
    val decoded = Json.decodeFromString(FooBar.serializer(), encoded)
    if (decoded !== FooBar) return "Incorrect object instance"
    return "OK"
}
