package algorithm

import RouteRequest
import objects.Car
import java.util.*
import java.util.concurrent.Exchanger

class RoomBypassingAlgorithm(thisCar: Car, exchanger: Exchanger<IntArray>) : AbstractAlgorithm(thisCar, exchanger) {

    private val MOVE_VELOCITY = 30.3//sm/s
    private val ROTATION_VELOCITY = 11.0//degrees/s


    private var wallLength = 0.0//in sm
    private var wallAngle = 0.0//in radian
    private var xPos = 0.0
    private var yPos = 0.0


    override fun getCarState(anglesDistances: Map<Int, Double>): CarState {
        val dist0 = anglesDistances[0]!!
        val dist90 = anglesDistances[90]!!
        val dist60 = anglesDistances[60]!!
        val oldDist90 = getPrevSonarDistances()[90]
        println(dist90)
        println(oldDist90)
        if (oldDist90 != null && oldDist90 + 60 < dist90) {
            return CarState.OUTER
        }
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
        when (state) {
            CarState.WALL -> {
                if (dist90 > 40 || dist90 < 20) {
                    val rotationDirection = if (dist90 > 40) RIGHT else LEFT
                    resultBuilder.setDirections(getIntArray(rotationDirection, FORWARD))
                    wallLength += 1 * MOVE_VELOCITY
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
                wallLength += 1 * MOVE_VELOCITY
                resultBuilder.setTimes(getIntArray(1000))
                return resultBuilder.build()
            }
            CarState.INNER -> {

                if (getPrevState() != state) {
                    val pointsNextLine = arrayListOf<Pair<Double, Double>>()
                    val pointsCurrentLine = arrayListOf<Pair<Double, Double>>()
                    for (angleDist in anglesDistances) {
                        if (angleDist.key <= 40) {
                            pointsNextLine.add(sonarAngleDistToPoint(angleDist.toPair()))
                        } else if (angleDist.key <= 120) {
                            pointsCurrentLine.add(sonarAngleDistToPoint(angleDist.toPair()))
                        }
                    }
                    val innerAnglePoint = getInnerAnglePoint(pointsNextLine, pointsCurrentLine)
                    val vectorNextLine = Vector(innerAnglePoint.first, innerAnglePoint.second,
                            pointsNextLine[0].first, pointsNextLine[0].second)


                    val vectorCurrentLine = Vector(innerAnglePoint.first, innerAnglePoint.second,
                            pointsCurrentLine.last().first, pointsCurrentLine.last().second)

                    val scalarMult = vectorCurrentLine.scalarProduct(vectorNextLine)
                    val angle = Math.acos(scalarMult / (vectorCurrentLine.length() * vectorNextLine.length()))
                    wallAngle += angle
                    //todo add length and save this point
                }

                resultBuilder.setDirections(getIntArray(LEFT, FORWARD, LEFT))
                resultBuilder.setTimes(getIntArray((45 * 1000 / ROTATION_VELOCITY).toInt(), 1000, (45 * 1000 / ROTATION_VELOCITY).toInt()))
                return resultBuilder.build()
            }
            CarState.OUTER -> {
                resultBuilder.setDirections(getIntArray(RIGHT, FORWARD, RIGHT))
                resultBuilder.setTimes(getIntArray((45 * 1000 / ROTATION_VELOCITY).toInt(), 1000, (45 * 1000 / ROTATION_VELOCITY).toInt()))
                return resultBuilder.build()
            }
        }
    }

    private fun getInnerAnglePoint(pointsNextLine: ArrayList<Pair<Double, Double>>,
                                   pointsCurrentLine: ArrayList<Pair<Double, Double>>): Pair<Double, Double> {

        val nextLine = approximatePointsByLine(pointsNextLine.toTypedArray())
        val currentLine = approximatePointsByLine(pointsCurrentLine.toTypedArray())
        val b1 = currentLine.second
        val k1 = currentLine.first
        val b2 = nextLine.second
        val k2 = nextLine.first
        val intersectionPoint = Pair((b1 - b2) / (k2 - k1), (k2 * b1 - b2 * k1) / (k2 - k1))
        return intersectionPoint
    }

    private fun degreesToRadian(angle: Int): Double {
        return Math.PI * angle / 180
    }

    private fun sonarAngleDistToPoint(angleDist: Pair<Int, Double>): Pair<Double, Double> {

        val angle = wallAngle - degreesToRadian(angleDist.first)
        val dist = angleDist.second

        val result = Pair(xPos + Math.cos(angle) * dist, yPos + Math.sin(angle) * dist)
        return result
    }

    //returns coef in y=kx+b (first is k, second is b)
    private fun approximatePointsByLine(points: Array<Pair<Double, Double>>): Pair<Double, Double> {

        val sumX = points.sumByDouble { it.first }
        val sumXQuad = points.sumByDouble { it.first * it.first }
        val sumY = points.sumByDouble { it.second }
        val sumXY = points.sumByDouble { it.second * it.first }

        val pointsCount = points.size
        val k = (pointsCount * sumXY - sumX * sumY) / (pointsCount * sumXQuad - sumX * sumX)
        val b = (sumY - sumX * k) / pointsCount

        return Pair(k, b)
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