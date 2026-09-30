value class Meters(val value: Int)

value class Point(val x: Int, val y: Int)

fun twice(meters: Meters): Meters = Meters(meters.value * 2)

fun sum(point: Point): Int = point.x + point.y
