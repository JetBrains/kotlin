// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
object FooBar

fun box(): String {
    return Json.encodeToString(FooBar.serializer(), FooBar)
}