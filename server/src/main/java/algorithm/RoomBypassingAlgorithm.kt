package algorithm

import algorithm.geometry.Line
import algorithm.geometry.Vector
import objects.Car
import RouteMetricRequest
import java.util.concurrent.Exchanger

class RoomBypassingAlgorithm(thisCar: Car, exchanger: Exchanger<IntArray>) : AbstractAlgorithm(thisCar, exchanger) {

    public var wallAngleWithOX = 0.0//in radian
    public var wallLength = 0.0//sm
    var errorCount = 0

    var carX = 0
    var carY = 0
    var carAngle = 0

    private var prevPoint = Pair(0.0, 0.0)

    override fun getCarState(anglesDistances: Map<Int, Double>): CarState? {
        val dist0 = anglesDistances[0]
        val dist90 = anglesDistances[90]
        if (dist90 == null || dist90 > 85) {
            requiredAngles = getIntArray(0, 60, 90, 120, 180)
            return CarState.OUTER
        }
        if (dist90 < 20) {
            return CarState.WALL
        }
        if (dist0 == null || dist0 > 70) {
            return CarState.WALL
        }
        return CarState.INNER
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

    override fun getCommand(anglesDistances: Map<Int, Double>, state: CarState): RouteMetricRequest {
        val dist0 = anglesDistances[0]
        val dist60 = anglesDistances[60]
        val dist90 = anglesDistances[90]
        val dist120 = anglesDistances[120]
        val dist180 = anglesDistances[180]
        val resultBuilder = RouteMetricRequest.BuilderRouteMetricRequest(IntArray(0), IntArray(0))
        if (dist120 == null || dist90 == null || dist60 == null) {
            println("null distance!")
            if (errorCount >= 3) {
                errorCount = 0
                resultBuilder.setDirections(getIntArray(BACKWARD))
                resultBuilder.setDistances(getIntArray(15))
                return resultBuilder.build()
            }
            errorCount++
            return resultBuilder.build()
        }
        if (getPrevState() == null) {
            //its first run, save right wall
            RoomModel.lines.add(Line(0.0, 1.0, dist90))
            prevPoint = Pair(0.0, -dist90)
            if (dist0 != null) {
                wallLength = dist0
            }
        }
        when (state) {
            CarState.WALL -> {
                if (wallLength.toInt() == 0 && (dist0 != null && dist180 != null)) {
                    wallLength = dist0 + dist180
                }
                if (dist90 > 40 || dist90 < 20) {
                    val rotationDirection = if (dist90 > 40) RIGHT else LEFT
                    resultBuilder.setDirections(getIntArray(rotationDirection, FORWARD))
                    resultBuilder.setDistances(getIntArray(10, 35))
                    return resultBuilder.build()
                }

                if (Math.abs(dist120 - dist60) > 10) {
                    val rotationDirection = if (dist120 > dist60) LEFT else RIGHT
                    resultBuilder.setDirections(getIntArray(rotationDirection))
                    resultBuilder.setDistances(getIntArray(15))
                    return resultBuilder.build()
                }

                resultBuilder.setDirections(getIntArray(FORWARD))
                resultBuilder.setDistances(getIntArray(35))
                return resultBuilder.build()
            }
            CarState.INNER -> {

                //в угле первое действие - встать максимально паралельно стене.
                if (Math.abs(dist120 - dist60) > 10) {
                    val rotationDirection = if (dist120 > dist60) LEFT else RIGHT
                    resultBuilder.setDirections(getIntArray(rotationDirection))
                    resultBuilder.setDistances(getIntArray(12))
                    return resultBuilder.build()
                }

                //если стоим паралельно - можно мерить угол
                val pointsNextLine = arrayListOf<Pair<Double, Double>>()
                for (angleDist in anglesDistances) {
                    if (angleDist.key <= 15) {
                        pointsNextLine.add(sonarAngleDistToPoint(angleDist.toPair()))
                    }
                }
                if (pointsNextLine.size < 3) {
                    requiredAngles = IntArray(37, { it * 5 })
                    return resultBuilder.build()
                }
                val nextLine = approximatePointsByLine(pointsNextLine.toTypedArray())
                val currentLine = RoomModel.lines.last()

                val xIntersection = prevPoint.first + wallLength * Math.cos(wallAngleWithOX)
                val yIntersection = prevPoint.second + wallLength * Math.sin(wallAngleWithOX)
                nextLine.C = -nextLine.A * xIntersection - nextLine.B * yIntersection

                var xOnNextLine = xIntersection + 1
                var yOnNextLine = (-nextLine.C - nextLine.A * xOnNextLine) / nextLine.B

                //точка должна находится слева от нашей правой стены
                if (currentLine.A * xOnNextLine + currentLine.B * yOnNextLine + currentLine.C < 0) {
                    xOnNextLine = xIntersection - 1
                    yOnNextLine = (-nextLine.C - nextLine.A * xOnNextLine) / nextLine.B
                }

                val vectorNextLine = Vector(xIntersection, yIntersection, xOnNextLine, yOnNextLine)
                val vectorCurrentLine = Vector(xIntersection, yIntersection, prevPoint.first, prevPoint.second)

                val scalarProduct = vectorCurrentLine.scalarProduct(vectorNextLine)
//                val angle = Math.acos(scalarProduct / (vectorCurrentLine.length() * vectorNextLine.length()))
                //todo
                val angle = Math.PI / 2
                val wallsAngleInDegrees = radiansToDegrees(angle)
                wallAngleWithOX += degreesToRadian(180 - wallsAngleInDegrees.toInt())
                prevPoint = Pair(xIntersection, yIntersection)
                wallLength = 0.0
                RoomModel.lines.add(nextLine)
                resultBuilder.setDirections(getIntArray(LEFT, FORWARD, LEFT))
                resultBuilder.setDistances(getIntArray((angle / 2).toInt(), 15, (angle / 2).toInt()))
                return resultBuilder.build()
            }
            CarState.OUTER -> {
                //todo calculate target
                resultBuilder.setDirections(getIntArray(RIGHT, FORWARD, RIGHT))
                resultBuilder.setDistances(getIntArray(45, 40, 45))
                return resultBuilder.build()
            }
        }
    }

    private fun degreesToRadian(angle: Int): Double {
        return Math.PI * angle / 180
    }

    private fun radiansToDegrees(angle: Double): Double {
        return angle * 180 / Math.PI
    }

    private fun sonarAngleDistToPoint(angleDist: Pair<Int, Double>): Pair<Double, Double> {

        val angle = wallAngleWithOX - degreesToRadian(angleDist.first)
        val dist = angleDist.second

        val result = Pair(Math.cos(angle) * dist, Math.sin(angle) * dist)
        return result
    }

    private fun approximatePointsByLine(points: Array<Pair<Double, Double>>): Line {

//        val p1 = points.first()
//        val p2 = points[1]
//        return Line(p2.second - p1.second, p1.first - p2.first, -p1.first * p2.second + p1.second * p2.first)

        val sumX = points.sumByDouble { it.first }
        val sumXQuad = points.sumByDouble { it.first * it.first }
        val sumY = points.sumByDouble { it.second }
        val sumXY = points.sumByDouble { it.second * it.first }

        val pointsCount = points.size
        val den = pointsCount * sumXQuad - sumX * sumX
        if (Math.abs(den) < 0.001) {
            return Line(1.0, 0.0, -points.first().first)
        }
        val k = (pointsCount * sumXY - sumX * sumY) / (pointsCount * sumXQuad - sumX * sumX)
        val b = (sumY - sumX * k) / pointsCount

        return Line(-k, 1.0, -b)
    }


    private fun calculateAngleWithWall(anglesDistances: MutableMap<Int, Double>): Double {
        val dist60 = anglesDistances[60]
        val dist120 = anglesDistances[120]

        //Math.cos(60) = 1/2
//        val wallLength = Math.sqrt(Math.pow(dist60, 2.0) + Math.pow(dist120, 2.0) - dist120 * dist60)//in triangle
//
//        val hOnWall = getRangeToWall(wallLength, dist60, dist120)
        return 0.0
    }

    //return height in triangle on side a
    private fun getRangeToWall(a: Double, b: Double, c: Double): Double {
        val halfPerimeter = (a + b + c) / 2
        return Math.sqrt(halfPerimeter * (halfPerimeter - a) * (halfPerimeter - b) * (halfPerimeter - c)) * 2 / a
    }


}