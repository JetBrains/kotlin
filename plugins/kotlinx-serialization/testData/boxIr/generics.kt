// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class Foo<T>(val i: Int, val t: T? = null)

@Serializable
class Holder(val f1: Foo<String>, val f2: Foo<Int>)

fun box(): String {
    val holder = Holder(Foo(1, "1"), Foo(2))
    val str = Json.encodeToString(Holder.serializer(), holder)
    if (str != """{"f1":{"i":1,"t":"1"},"f2":{"i":2}}""") return str
    val decoded = Json.decodeFromString(Holder.serializer(), str)
    if (decoded.f1.t != holder.f1.t) return "f1.t: ${decoded.f1.t}"
    return "OK"
}
