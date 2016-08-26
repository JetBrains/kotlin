package algorithm

import objects.Car
import java.util.concurrent.Exchanger
import RouteRequest

class RoomTest(thisCar: Car, exchanger: Exchanger<IntArray>) : AbstractAlgorithm(thisCar, exchanger) {

    private val MOVE_VELOCITY = 0.05//sm/ms
    private val ROTATION_VELOCITY = 0.05//degrees/ms

    val points = arrayListOf<Pair<Double, Double>>()


    var carX = 0
    var carY = 0
    var carAngle = 0

    override fun getCarState(anglesDistances: Map<Int, Double>): CarState {
        return CarState.WALL
    }

    override fun getCommand(anglesDistances: Map<Int, Double>, state: CarState): RouteRequest {
        val dist0 = anglesDistances[0]
        val dist60 = anglesDistances[60]
        val dist90 = anglesDistances[90]
        val dist120 = anglesDistances[120]
        val resultBuilder = RouteRequest.BuilderRouteRequest(IntArray(0), IntArray(0))
        if (dist90 == null) {
            resultBuilder.setDirections(getIntArray(RIGHT))
            resultBuilder.setTimes(getIntArray((10 / ROTATION_VELOCITY).toInt()))
            return resultBuilder.build()
        }
        val sonarAngle = carAngle - 90
        points.add(Pair(carX + dist90 * Math.cos(degreesToRadian(sonarAngle)),
                carY + dist90 * Math.sin(degreesToRadian(sonarAngle))))
        if (dist120 == null || dist60 == null) {
            //hmm
            resultBuilder.setDirections(getIntArray(FORWARD))
            resultBuilder.setTimes(getIntArray((10 / MOVE_VELOCITY).toInt()))
            return resultBuilder.build()
        }
        if (dist0 != null && dist0 < 60) {
            resultBuilder.setDirections(getIntArray(LEFT, FORWARD))
            resultBuilder.setTimes(getIntArray((20 / ROTATION_VELOCITY).toInt(), (10 / MOVE_VELOCITY).toInt()))
            return resultBuilder.build()
        }
        val abs = Math.abs(dist120 - dist60)
        if (abs > 10) {
            val rotationDirection = if (dist120 > dist60) LEFT else RIGHT
            resultBuilder.setDirections(getIntArray(rotationDirection, FORWARD))
            resultBuilder.setTimes(getIntArray((10.0 / ROTATION_VELOCITY).toInt(), (7 / MOVE_VELOCITY).toInt()))//todo calibrate
            return resultBuilder.build()
        }
        if (dist90 > 50 || dist90 < 25) {
            val rotationDirection = if (dist90 > 50) RIGHT else LEFT
            resultBuilder.setDirections(getIntArray(rotationDirection, FORWARD))
            resultBuilder.setTimes(getIntArray((10 / ROTATION_VELOCITY).toInt(), (10 / MOVE_VELOCITY).toInt()))
            return resultBuilder.build()
        }

        resultBuilder.setDirections(getIntArray(FORWARD))
        resultBuilder.setTimes(getIntArray((20 / ROTATION_VELOCITY).toInt()))
        return resultBuilder.build()
    }

    private fun getIntArray(vararg args: Int): IntArray {
        return args
    }

    override fun afterGetCommand(route: RouteRequest) {
        route.directions.forEachIndexed { idx, direction ->
            when (direction) {
                FORWARD -> {
                    carX += (MOVE_VELOCITY * route.times[idx] * Math.cos(degreesToRadian(carAngle.toInt()))).toInt()
                    carY += (MOVE_VELOCITY * route.times[idx] * Math.sin(degreesToRadian(carAngle.toInt()))).toInt()
                }
                BACKWARD -> {
                    carX -= (MOVE_VELOCITY * route.times[idx] * Math.cos(degreesToRadian(carAngle.toInt()))).toInt()
                    carY -= (MOVE_VELOCITY * route.times[idx] * Math.sin(degreesToRadian(carAngle.toInt()))).toInt()
                }
                LEFT -> {
                    carAngle += (ROTATION_VELOCITY * route.times[idx]).toInt()
                }
                RIGHT -> {
                    carAngle -= (ROTATION_VELOCITY * route.times[idx]).toInt()
                }
            }
        }
    }

    private fun degreesToRadian(angle: Int): Double {
        return Math.PI * angle / 180
    }

}