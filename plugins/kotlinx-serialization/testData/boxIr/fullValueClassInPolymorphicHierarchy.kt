// LANGUAGE: +FullValueClasses
// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
sealed interface Shape

@Serializable
value class Circle(val radius: Int) : Shape

@Serializable
data class Square(val side: Int) : Shape

@Serializable
class Holder(val shape: Shape, val shapes: List<Shape>, val circle: Circle)

fun box(): String {
    val holder = Holder(Circle(6), listOf(Square(1), Circle(2)), Circle(3))
    val encoded = Json.encodeToString(holder)
    val expected = """{"shape":{"type":"Circle","radius":6},"shapes":[{"type":"Square","side":1},{"type":"Circle","radius":2}],"circle":{"radius":3}}"""
    if (encoded != expected) return "encoded: $encoded"
    val decoded = Json.decodeFromString<Holder>(encoded)
    if (decoded.shape != holder.shape || decoded.shapes != holder.shapes || decoded.circle != holder.circle) return "decoded"
    return "OK"
}
