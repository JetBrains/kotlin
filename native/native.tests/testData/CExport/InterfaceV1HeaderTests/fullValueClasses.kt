// LANGUAGE: +FullValueClasses
// ISSUE: KT-91357
// A single-field full value class is exported as a class, like a multi-field one, and an inline value class as its underlying type.

value class Meters(val value: Int)

inline value class InlineMeters(val value: Int)

value class Point(val x: Int, val y: Int)

fun twice(meters: Meters): Meters = Meters(meters.value * 2)

fun twiceInline(meters: InlineMeters): InlineMeters = InlineMeters(meters.value * 2)

fun sum(point: Point): Int = point.x + point.y

fun nullable(meters: Meters?): Meters? = meters

fun nullableInline(meters: InlineMeters?): InlineMeters? = meters

fun Meters.doubled(): Int = value * 2

fun InlineMeters.doubledInline(): Int = value * 2

val zero: Meters get() = Meters(0)

val zeroInline: InlineMeters get() = InlineMeters(0)
