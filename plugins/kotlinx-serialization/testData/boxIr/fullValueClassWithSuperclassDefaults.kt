// TARGET_BACKEND: JVM
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
sealed value class Shape

@Serializable
value class Circle(val radius: Double = 1.0, @Transient val diameter: Double = radius * 2) : Shape()

fun box(): String {
    val circle = Json.decodeFromString<Circle>("{}")
    if (circle != Circle() || circle.diameter != 2.0) return "decoded: $circle"
    val encoded = Json.encodeToString(Circle())
    if (encoded != "{}") return "encoded: $encoded"
    val shape = Json.decodeFromString<Shape>("""{"type":"Circle","radius":2.0}""")
    if (shape != Circle(2.0) || (shape as Circle).diameter != 4.0) return "decoded: $shape"
    return "OK"
}
