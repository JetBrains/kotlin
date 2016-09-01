package algorithm.geometry

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
    val MAX_SLOPE = 0.2

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

        var sumx = 0.0
        var sumy = 0.0
        var sumx2 = 0.0
        for ((x2, y2) in rawPoints) {
            sumx += x2
            sumx2 += x2 * x2
            sumy += y2
            n++
        }
        val xbar = sumx / n
        val ybar = sumy / n

        // second pass: compute summary statistics
        var xxbar = 0.0
        var yybar = 0.0
        var xybar = 0.0
        for ((x1, y1) in rawPoints) {
            xxbar += (x1 - xbar) * (x1 - xbar)
            yybar += (y1 - ybar) * (y1 - ybar)
            xybar += (x1 - xbar) * (y1 - ybar)
        }

        var beta1 = xybar / xxbar
        var beta0 = ybar - beta1 * xbar

        if (Math.abs(beta1) > MAX_SLOPE) {
            beta1 = xybar / yybar
            beta0 = xbar - beta1 * ybar
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

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is Wall) return false
        return line.equals(other.line)
    }
}