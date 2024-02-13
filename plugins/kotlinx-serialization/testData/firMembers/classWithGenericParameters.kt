// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.builtins.*

@Serializable
class GenericBox<T, V>(
    val i: Int,
    val t: T,
    val vs: List<V>
)

fun box(): String {
    val box = GenericBox(42, "foo", listOf(true, false))
    val serial = GenericBox.serializer(String.serializer(), Boolean.serializer())
    val target = """{"i":42,"t":"foo","vs":[true,false]}"""
    val s = Json.encodeToString(serial, box)
    if (target != s) return "Incorrect serialization: $s"
    val decoded = Json.decodeFromString(serial, s)
    if (box.t != decoded.t || box.vs != decoded.vs) return "Incorrect deserialization"
    return "OK"
}
