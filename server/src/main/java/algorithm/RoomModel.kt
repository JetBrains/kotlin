package algorithm
import Waypoints
import algorithm.geometry.Line

object RoomModel {

    val point = arrayListOf<Pair<Double, Double>>()
    val lines = arrayListOf<Line>()


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
}