package objects

import roomScanner.CarController.Direction
import RouteMetricRequest
import algorithm.geometry.Angle

class Car constructor(val uid: Int, host: String, port: Int) {

    private val CHARGE_CORRECTION = 1.0//on full charge ok is 0.83 - 0.86

    var x = 0.0
    var y = 0.0
    var angle = 0.0
    val carConnection = CarConnection(host, port)

    fun refreshPositionAfterRoute(route:RouteMetricRequest) {
        val carAngle = angle.toInt()
        route.directions.forEachIndexed { idx, direction ->
            when (direction) {
                Direction.FORWARD.id -> {
                    x += (Math.cos(Angle(carAngle).rads()) * route.distances[idx]).toInt()
                    y += (Math.sin(Angle(carAngle).rads()) * route.distances[idx]).toInt()
                }
                Direction.BACKWARD.id -> {
                    x -= (Math.cos(Angle(carAngle).rads()) * route.distances[idx]).toInt()
                    y -= (Math.sin(Angle(carAngle).rads()) * route.distances[idx]).toInt()
                }
                Direction.LEFT.id -> {
                    route.distances[idx] = (CHARGE_CORRECTION * route.distances[idx]).toInt()
                }
                Direction.RIGHT.id -> {
                    route.distances[idx] = (CHARGE_CORRECTION * route.distances[idx]).toInt()
                }
            }
        }
    }

    override fun toString(): String {
        return "$uid ; x:$x; y:$y; target:$angle"
    }
}