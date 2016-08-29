package algorithm.geometry

import java.util.*

data class Wall(val points: ArrayList<Point> = arrayListOf<Point>(),
                var line: Line = Line(0.0, 0.0, 0.0)) {


    fun addPoint(point: Point) {
        points.add(point)
        line = approximatePointsByLine()
    }

    private fun approximatePointsByLine(): Line {

        val sumX = points.sumByDouble { it.x }
        val sumXQuad = points.sumByDouble { it.x * it.x }
        val sumY = points.sumByDouble { it.y }
        val sumXY = points.sumByDouble { it.y * it.x }

        val pointsCount = points.size
        val den = pointsCount * sumXQuad - sumX * sumX

        if (Math.abs(den) < 0.001) {
            return Line(1.0, 0.0, -points.first().x)
        }

        val k = (pointsCount * sumXY - sumX * sumY) / (pointsCount * sumXQuad - sumX * sumX)
        val b = (sumY - sumX * k) / pointsCount

        return Line(-k, 1.0, -b)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (!(other is Wall)) return false
        return line.equals(other.line)
    }
}