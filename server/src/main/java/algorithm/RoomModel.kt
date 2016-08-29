package algorithm

import DebugClInterface
import DebugResponse
import Waypoints
import algorithm.geometry.Line
import objects.Car

object RoomModel {

    val lines = arrayListOf<Line>()

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

        val algorithm = DebugClInterface.algorithmImpl
        if (algorithm == null || !(algorithm is RoomBypassingAlgorithm)) {
            val emptyArr = IntArray(0)
            return Waypoints.BuilderWaypoints(emptyArr, emptyArr, emptyArr, emptyArr, false).build()

        }
        val begin_x = IntArray(algorithm.points.size)
        val begin_y = IntArray(algorithm.points.size)
        val end_x = IntArray(algorithm.points.size)
        val end_y = IntArray(algorithm.points.size)


        for (i in 0..algorithm.points.size - 2) {
            val curPoint = algorithm.points[i]
            val nextPoint = algorithm.points[i + 1]
            begin_x[i] = (curPoint.x + 0.5).toInt()
            begin_y[i] = (curPoint.y + 0.5).toInt()

            end_x[i] = (nextPoint.x + 0.5).toInt()
            end_y[i] = (nextPoint.y + 0.5).toInt()
        }
        return Waypoints.BuilderWaypoints(begin_x, begin_y, end_x, end_y, false).build()
    }

    fun getDebugInfo(): DebugResponse {

        val aRef = IntArray(linesModel.size)
        val bRef = IntArray(linesModel.size)
        val cRef = IntArray(linesModel.size)
        val a = IntArray(lines.size)
        val b = IntArray(lines.size)
        val c = IntArray(lines.size)

        val alg = DebugClInterface.algorithmImpl
        val car = if (alg == null) Car(0, "", 0) else alg.thisCar
        val mult = 100
        linesModel.forEachIndexed { idx, line ->
            aRef[idx] = (line.A * mult).toInt()
            bRef[idx] = (line.B * mult).toInt()
            cRef[idx] = (line.C * mult).toInt()
        }
        lines.forEachIndexed { idx, line ->
            a[idx] = (line.A * mult).toInt()
            b[idx] = (line.B * mult).toInt()
            c[idx] = (line.C * mult).toInt()
        }

        val resultBuilder = DebugResponse.BuilderDebugResponse(a, b, c, car.x, car.y, car.angle, aRef, bRef, cRef)
        return resultBuilder.build()
    }
}