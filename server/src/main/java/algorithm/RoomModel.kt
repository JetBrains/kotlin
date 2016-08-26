package algorithm

import Waypoints
import algorithm.geometry.Line
import DebugResponse
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
        if (iter == 5) {
            web.server.Server.changeMode(web.server.Server.ServerMode.IDLE)
            return Waypoints.BuilderWaypoints(IntArray(0), IntArray(0), IntArray(0), IntArray(0), true).build()
        }

        val begin_x = IntArray(10)
        val begin_y = IntArray(10)
        val end_x = IntArray(10)
        val end_y = IntArray(10)

        for (i in 0..9) {
            begin_x[i] = currentPosition_x
            begin_y[i] = currentPosition_y

            currentPosition_x += rng.nextInt(3)
            currentPosition_y += rng.nextInt(3)

            end_x[i] = currentPosition_x
            end_y[i] = currentPosition_y
        }
        iter++
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
        val mult = 1000000
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