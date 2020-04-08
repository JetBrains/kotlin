
fun <caret>sqr(x: Double) = x * x

class Point(val x: Double, val y: Double) {
    // After sqr inlining, only first usage of sqr is replaced
    fun distance(other: Point) = Math.sqrt(sqr(x - other.x) + sqr(y - other.y))
}