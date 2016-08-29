package algorithm

import objects.Car
import java.util.concurrent.Exchanger
import RouteMetricRequest
import algorithm.geometry.AngleData
import algorithm.geometry.Point

class RoomBypassingAlgorithm(thisCar: Car, exchanger: Exchanger<IntArray>) : AbstractAlgorithm(thisCar, exchanger) {

    val points = arrayListOf<Point>()
    private val DISTANCE_TO_WALL_THRESHOLD = 60

    var carX = 0
    var carY = 0
    var carAngle = 0

    override fun getCarState(anglesDistances: Map<Int, AngleData>): CarState {
        return CarState.WALL
    }

    override fun getCommand(anglesDistances: Map<Int, AngleData>, state: CarState): RouteMetricRequest {
        val dist0 = anglesDistances[0]
        val dist60 = anglesDistances[60]
        val dist90 = anglesDistances[90]
        val dist120 = anglesDistances[120]
        val resultBuilder = RouteMetricRequest.BuilderRouteMetricRequest(IntArray(0), IntArray(0))
        if (dist90 == null) {
            resultBuilder.setDirections(getIntArray(RIGHT))
            resultBuilder.setDistances(getIntArray(10))
            return resultBuilder.build()
        }
        val sonarAngle = carAngle - 90
        points.add(Point(
                x = carX + dist90.distance * Math.cos(degreesToRadian(sonarAngle)),
                y = carY + dist90.distance * Math.sin(degreesToRadian(sonarAngle))
        ))
        if (dist120 == null || dist60 == null) {
            resultBuilder.setDirections(getIntArray(FORWARD))
            resultBuilder.setDistances(getIntArray(10))
            return resultBuilder.build()
        }
        if (dist0 != null && dist0.distance < DISTANCE_TO_WALL_THRESHOLD) {
            resultBuilder.setDirections(getIntArray(LEFT, FORWARD))
            resultBuilder.setDistances(getIntArray(20, 10))
            return resultBuilder.build()
        }
        val abs = Math.abs(dist120.distance - dist60.distance)
        if (abs > 10) {
            val rotationDirection = if (dist120.distance > dist60.distance) LEFT else RIGHT
            resultBuilder.setDirections(getIntArray(rotationDirection, FORWARD))
            resultBuilder.setDistances(getIntArray(10, 7))//todo calibrate
            return resultBuilder.build()
        }
        if (dist90.distance > 50 || dist90.distance < 25) {
            val rotationDirection = if (dist90.distance > 50) RIGHT else LEFT
            resultBuilder.setDirections(getIntArray(rotationDirection, FORWARD))
            resultBuilder.setDistances(getIntArray(10, 10))
            return resultBuilder.build()
        }

        resultBuilder.setDirections(getIntArray(FORWARD))
        resultBuilder.setDistances(getIntArray(20))
        return resultBuilder.build()
    }

    private fun getIntArray(vararg args: Int): IntArray {
        return args
    }

    override fun afterGetCommand(route: RouteMetricRequest) {
        route.directions.forEachIndexed { idx, direction ->
            when (direction) {
                FORWARD -> {
                    carX += (Math.cos(degreesToRadian(carAngle.toInt())) * route.distances[idx]).toInt()
                    carY += (Math.sin(degreesToRadian(carAngle.toInt())) * route.distances[idx]).toInt()
                }
                BACKWARD -> {
                    carX -= (Math.cos(degreesToRadian(carAngle.toInt())) * route.distances[idx]).toInt()
                    carY -= (Math.sin(degreesToRadian(carAngle.toInt())) * route.distances[idx]).toInt()
                }
                LEFT -> {
                    carAngle += route.distances[idx]
                }
                RIGHT -> {
                    carAngle -= route.distances[idx]
                }
            }
        }
    }

    private fun degreesToRadian(angle: Int): Double {
        return Math.PI * angle / 180
    }

}