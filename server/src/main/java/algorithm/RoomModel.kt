package algorithm

import DebugClInterface
import DebugResponse
import Waypoints
import algorithm.geometry.Angle
import algorithm.geometry.Line
import algorithm.geometry.Point
import algorithm.geometry.Wall

object RoomModel {
    val walls = arrayListOf(Wall(Angle(0)))
    var finished = false

    val linesModel = listOf(Line(0.0, 1.0, -300.0),
            Line(1.0, 0.0, 150.0),
            Line(0.0, 1.0, 20.0),
            Line(1.0, 0.0, -200.0))


    //  DEBUG BELOW
    val rng = java.util.Random(42)
    var iter = 0
    var currentPosition_x = 0
    var currentPosition_y = 0
    fun getUpdate(): Waypoints {

        val points = getWallsPoints()

        val begin_x = IntArray(points.size)
        val begin_y = IntArray(points.size)
        val end_x = IntArray(points.size)
        val end_y = IntArray(points.size)


        for (i in 0..points.size - 2) {
            val curPoint = points[i]
            val nextPoint = points[i + 1]
            begin_x[i] = (curPoint.x + 0.5).toInt()
            begin_y[i] = (curPoint.y + 0.5).toInt()

            end_x[i] = (nextPoint.x + 0.5).toInt()
            end_y[i] = (nextPoint.y + 0.5).toInt()
        }
        return Waypoints.BuilderWaypoints(begin_x, begin_y, end_x, end_y, false).build()
    }

    private fun getWallsPoints(): Array<Point> {
        return walls.flatMap({ it.points }).toTypedArray()
    }

    fun updateWalls() {
        if (walls.size < 2) {
            // no walls to intersect
            return
        }
        val line1 = walls.last().line
        val line2 = walls[walls.size - 2].line

        val intersection: Point = line1.intersect(line2)


        val lastWall = walls[walls.size - 1]
        lastWall.pushFrontPoint(intersection)
        if (lastWall.isHorizontal()) {
            lastWall.line = Line(0.0, 1.0, -intersection.y)
        } else if (lastWall.isVertical()) {
            lastWall.line = Line(1.0, 0.0, -intersection.x)
        }

        walls[walls.size - 2].pushBackPoint(intersection)
        walls[walls.size - 2].markAsFinished()
    }

    fun getDebugInfo(): DebugResponse {

        val points = getWallsPoints()

        val begin_x = IntArray(points.size)
        val begin_y = IntArray(points.size)
        val end_x = IntArray(points.size)
        val end_y = IntArray(points.size)

        val rawPoints = walls.flatMap({ it.rawPoints }).toTypedArray()
        val pointsX = rawPoints.map { it.x.toInt() }.toIntArray()
        val pointsY = rawPoints.map { it.y.toInt() }.toIntArray()

        for (i in 0..points.size - 2) {
            val curPoint = points[i]
            val nextPoint = points[i + 1]
            begin_x[i] = (curPoint.x + 0.5).toInt()
            begin_y[i] = (curPoint.y + 0.5).toInt()

            end_x[i] = (nextPoint.x + 0.5).toInt()
            end_y[i] = (nextPoint.y + 0.5).toInt()
        }

        val wallDistances = walls.filter { it.isFinished }.map {
            val firstPoint = it.points.first()
            val lastPoint = it.points.last()
            val vector = algorithm.geometry.Vector(firstPoint, lastPoint)
            vector.length().toInt()
        }.toIntArray()

        val alg = DebugClInterface.algorithmImpl
        if (alg is RoomBypassingAlgorithm) {
            return DebugResponse.BuilderDebugResponse(begin_x, begin_y, end_x, end_y, alg.carX,
                    alg.carY, alg.carAngle, pointsX, pointsY, wallDistances).build()
        }
        return DebugResponse.BuilderDebugResponse(begin_x, begin_y, end_x, end_y, 0,
                0, 0, pointsX, pointsY, wallDistances).build()

    }
}