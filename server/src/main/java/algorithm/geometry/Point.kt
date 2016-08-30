package algorithm.geometry

data class Point(val x: Double, val y: Double) {
    operator fun plus(other: Vector): Point {
        return Point(x + other.x, y + other.y)
    }
}