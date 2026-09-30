// LANGUAGE: +FullValueClasses

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
value class Point(val x: Int, val y: Int)

@Serializable
value class Name(val value: String)

fun box(): String {
    if (!Point::class.java.isValue) return "Point is not a value class"
    val point = Json.decodeFromString<Point>("""{"x":1,"y":2}""")
    if (point != Point(1, 2)) return "Point: $point"
    if (Json.encodeToString(point) != """{"x":1,"y":2}""") return Json.encodeToString(point)
    val name = Json.decodeFromString<Name>("""{"value":"a"}""")
    if (name != Name("a")) return "Name: $name"
    return "OK"
}
