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
    val MAX_REGRESSION_ERROR = 10.0

    fun pushBackPoint(point: Point) {
        rawPoints.add(point)
        line = approximatePointsByLine()
    }

    fun pushFrontPoint(point: Point) {
        rawPoints.add(0, point)
        line = approximatePointsByLine()
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

        // analyze results
        val df = n - 2
        var rss = 0.0      // residual sum of squares
        var ssr = 0.0      // regression sum of squares
        for ((x, y) in rawPoints) {
            val fit = beta1 * x + beta0
            rss += (fit - y) * (fit - y)
            ssr += (fit - ybar) * (fit - ybar)
        }

        val svar = rss / df
        val svar1 = svar / xxbar

        if (svar1.isNaN() || svar1.isInfinite() || Math.sqrt(svar1).gt(MAX_REGRESSION_ERROR) || Math.abs(beta1) > 1) {
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