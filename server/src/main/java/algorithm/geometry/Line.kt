package algorithm.geometry

class Line(var A: Double, var B: Double, var C: Double) {

    init {
        normalize()
    }

    fun intersect(lineTwo: Line): Point {
        val slope = this.A * lineTwo.B - this.B * lineTwo.A
        if (Math.abs(slope) < 0.001) {
            throw ArithmeticException("lines is parallel")
        }
        val xIntersection = (this.B * lineTwo.C - lineTwo.B * this.C) / slope
        val yIntersection = (this.C * lineTwo.A - lineTwo.C * this.A) / slope
        return Point(xIntersection, yIntersection)
    }

    override fun toString(): String {
        return "Line(A=$A, B=$B, C=$C)"
    }

    fun normalize() {
        val div = Math.sqrt(A * A + B * B)
        A /= div
        B /= div
        C /= div

        if (A.eq(0.0)) {
            if (B.lt(0.0)) {
                A *= -1
                B *= -1
                C *= -1
            }
        } else {
            if (A.lt(0.0)) {
                A *= -1
                B *= -1
                C *= -1
            }
        }
    }

    fun getDirectionVector(): Vector {
        return Vector(A, -B)
    }

}