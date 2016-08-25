package algorithm.geometry

class Line(val A: Double, val B: Double, var C: Double) {


    fun getIntersectionPoint(lineTwo: Line): Pair<Double, Double> {
        val slope = this.A * lineTwo.B - this.B * lineTwo.A
        if (Math.abs(slope) < 0.001) {
            throw ArithmeticException("lines is parallel")
        }
        val xIntersection = (this.B * lineTwo.C - lineTwo.B * this.C) / slope
        val yIntersection = (this.C * lineTwo.A - lineTwo.C * this.A) / slope
        return Pair(xIntersection, yIntersection)
    }

    override fun toString(): String{
        return "Line(A=$A, B=$B, C=$C)"
    }


}