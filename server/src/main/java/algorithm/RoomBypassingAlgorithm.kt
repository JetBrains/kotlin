package algorithm

import RouteRequest
import objects.Car
import java.util.concurrent.Exchanger

class RoomBypassingAlgorithm(thisCar: Car, exchanger: Exchanger<IntArray>) : AbstractAlgorithm(thisCar, exchanger) {

    private val MOVE_VELOCITY = 30.3//sm/s
    private val ROTATION_VELOCITY = 11.0//degrees/s


    override fun getCarState(anglesDistances: Map<Int, Double>): CarState {
        val dist0 = anglesDistances[0]!!
        val dist90 = anglesDistances[90]!!
        if (dist90 < 20) {
            return CarState.WALL
        }
        if (dist0 > 60) {
            return CarState.WALL
        }
        return CarState.INNER
    }

    private fun getIntArray(vararg args: Int): IntArray {
        return args
    }

    override fun getAngles(): IntArray {
        return IntArray(19, { it * 10 })
    }

    override fun getCommand(anglesDistances: Map<Int, Double>, state: CarState): RouteRequest {
        val dist0 = anglesDistances[0]!!
        val dist60 = anglesDistances[60]!!
        val dist90 = anglesDistances[90]!!
        val dist120 = anglesDistances[120]!!
        val resultBuilder = RouteRequest.BuilderRouteRequest(IntArray(0), IntArray(0))
        if (state == CarState.WALL) {
            if (dist90 > 40 || dist90 < 20) {
                val rotationDirection = if (dist90 > 40) RIGHT else LEFT
                resultBuilder.setDirections(getIntArray(rotationDirection, FORWARD))
                resultBuilder.setTimes(getIntArray(2000, 1000))
                return resultBuilder.build()
            }

            if (Math.abs(dist120 - dist60) > 10) {
                val rotationDirection = if (dist120 > dist60) LEFT else RIGHT
                resultBuilder.setDirections(getIntArray(rotationDirection))
                resultBuilder.setTimes(getIntArray(1000))
                return resultBuilder.build()
            }

            resultBuilder.setDirections(getIntArray(FORWARD))
            resultBuilder.setTimes(getIntArray(1000))
            return resultBuilder.build()

        } else {

            resultBuilder.setDirections(getIntArray(LEFT, FORWARD, LEFT))
            resultBuilder.setTimes(getIntArray((45 * 1000 / ROTATION_VELOCITY).toInt(), 1000, (45 * 1000 / ROTATION_VELOCITY).toInt()))
            return resultBuilder.build()
        }
    }

    private fun calculateAngleWithWall(anglesDistances: MutableMap<Int, Double>): Double {
        val dist60 = anglesDistances[60]!!
        val dist120 = anglesDistances[120]!!

        //Math.cos(60) = 1/2
        val wallLength = Math.sqrt(Math.pow(dist60, 2.0) + Math.pow(dist120, 2.0) - dist120 * dist60)//in triangle

        val hOnWall = getRangeToWall(wallLength, dist60, dist120)
        return 0.0
    }

    //return height in triangle on side a
    private fun getRangeToWall(a: Double, b: Double, c: Double): Double {
        val halfPerimeter = (a + b + c) / 2
        return Math.sqrt(halfPerimeter * (halfPerimeter - a) * (halfPerimeter - b) * (halfPerimeter - c)) * 2 / a
    }


}