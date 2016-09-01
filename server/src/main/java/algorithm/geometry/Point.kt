package algorithm.geometry

data class Point(var x: Double, var y: Double) {
    operator fun plus(other: Vector): Point {
        return Point(x + other.x, y + other.y)
    }
}