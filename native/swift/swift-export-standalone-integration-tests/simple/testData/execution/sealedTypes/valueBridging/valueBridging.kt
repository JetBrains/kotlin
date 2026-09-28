// KIND: STANDALONE
// MODULE: SealedValueBridging
// FILE: main.kt

// API positions have no sealed-specific translation, but `sealedType()` dispatches with `as` casts
// on the Swift wrapper, so it depends on how the value reached Swift.
sealed interface Shape

class Circle : Shape {
    override fun toString(): String = "Circle"
}

class Oval : Shape {
    override fun toString(): String = "Oval"
}

fun asReturn(): Shape = Circle()

fun asNullableReturn(empty: Boolean): Shape? = if (empty) null else Oval()

fun asListReturn(): List<Shape> = listOf(Circle(), Oval())

fun asMapValueReturn(): Map<String, Shape> = mapOf("circle" to Circle(), "oval" to Oval())

fun asCallbackArgument(transform: (Shape) -> String): String = transform(Oval())

class Holder {
    var state: Shape = Circle()
}
