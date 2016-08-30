package algorithm.geometry

class Vector constructor(var x: Double, var y: Double) {

    constructor(x1: Double, y1: Double, x2: Double, y2: Double) : this(x2 - x1, y2 - y1)

    constructor(begin: Point, end: Point) : this (begin.x, begin.y, end.x, end.y)

    fun scalarProduct(vector: Vector): Double {
        return this.x * vector.x + this.y * vector.y
    }

    fun length(): Double {
        return Math.sqrt(x * x + y * y)
    }

    fun angleTo(other: Vector): Angle {
        var sp = scalarProduct(other)
        var cosinus = sp / length() / other.length()
        return Angle(Math.acos(cosinus))
    }

    fun inverse() {
        x *= -1
        y *= -1
    }

    fun normalize() {
        val div = Math.sqrt(x * x + y * y)

        x /= div
        y /= div
    }
}
