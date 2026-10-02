// LANGUAGE: +FullValueClasses
// JVM_EXPOSE_BOXED

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@JvmInline
@Serializable
value class Wrapper(val s: String)

@Serializable
value class Holder(val x: Int, val w: Wrapper? = null)

fun box(): String {
    val holder = Json.decodeFromString<Holder>("""{"x":1,"w":"a"}""")
    if (holder != Holder(1, Wrapper("a"))) return "decoded: $holder"
    val empty = Json.decodeFromString<Holder>("""{"x":2}""")
    if (empty != Holder(2)) return "decoded: $empty"
    return "OK"
}
