package algorithm.geometry

import Logger
import java.util.*

data class Wall(val wallAngleOX: Angle,
                val rawPoints: ArrayList<Point> = arrayListOf<Point>(),
                var line: Line = Line(0.0, 0.0, 0.0)
) {
    val id: Int

    companion object {
        var idCounter = 0
    }

    init {
        id = idCounter
        idCounter++
    }

    var isFinished = false
    private val MAX_SLOPE = 0.2

    fun pushBackPoint(point: Point) {
        Logger.log("Adding ${point.toString()}")
        rawPoints.add(point)
        Logger.log("Line before approximation ${line.toString()}")
        line = approximatePointsByLine()
        Logger.log("Line after approximation ${line.toString()}")
    }

    fun pushFrontPoint(point: Point) {
        Logger.log("Adding ${point.toString()}")
        rawPoints.add(0, point)
        Logger.log("Line before approximation ${line.toString()}")
        line = approximatePointsByLine()
        Logger.log("Line after approximation ${line.toString()}")
    }

    var points: ArrayList<Point> = arrayListOf()

    fun markAsFinished() {
        Logger.log("Marking wall with id = ${this.id} as finished")
        isFinished = true
        points = generatePoints()
    }

    private fun generatePoints(): ArrayList<Point> {
        val direction = Vector(rawPoints.first(), rawPoints.last())
        val res: ArrayList<Point> = arrayListOf()

        val pointsCount = direction.length().toInt()

        direction.normalize()

        var curPoint = rawPoints.first()
        res.add(curPoint)
        val endPoint = rawPoints.last()
        for (i in 0..pointsCount - 1) {
            curPoint += direction
            res.add(curPoint)
        }
        res.add(endPoint)

        return res
    }

    private fun approximatePointsByLine(): Line {
        var n = 0

        var sumX = 0.0
        var sumY = 0.0
        var sumX2 = 0.0
        for ((x2, y2) in rawPoints) {
            sumX += x2
            sumX2 += x2 * x2
            sumY += y2
            n++
        }
        val xBar = sumX / n
        val yBar = sumY / n

        // second pass: compute summary statistics
        var xxbar = 0.0
        var yybar = 0.0
        var xybar = 0.0
        for ((x1, y1) in rawPoints) {
            xxbar += (x1 - xBar) * (x1 - xBar)
            yybar += (y1 - yBar) * (y1 - yBar)
            xybar += (x1 - xBar) * (y1 - yBar)
        }

        var beta1 = xybar / xxbar
        var beta0 = yBar - beta1 * xBar

        if (Math.abs(beta1) > MAX_SLOPE) {
            beta1 = xybar / yybar
            beta0 = xBar - beta1 * yBar
            return Line(1.0, -beta1, -beta0)
        }

        return Line(-beta1, 1.0, -beta0)
    }

    fun isVertical(): Boolean {
        return Math.abs(wallAngleOX.degs()) % 180 == 90
    }

    fun isHorizontal(): Boolean {
        return Math.abs(wallAngleOX.degs()) % 180 == 0
    }
}