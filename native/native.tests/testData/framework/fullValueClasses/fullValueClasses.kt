// ISSUE: KT-89981
value class Meters(val value: Int)

inline value class InlineMeters(val value: Int)

value class Point(val x: Int, val y: Int)

fun twice(meters: Meters): Meters = Meters(meters.value * 2)

fun twiceInline(meters: InlineMeters): InlineMeters = InlineMeters(meters.value * 2)

fun sum(point: Point): Int = point.x + point.y
