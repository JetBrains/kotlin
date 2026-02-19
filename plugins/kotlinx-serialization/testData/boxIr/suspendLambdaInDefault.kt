// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*

@Serializable
class Box(
    val block: suspend () -> Unit = {}
)

fun box(): String {
    Box()
    return "OK"
}