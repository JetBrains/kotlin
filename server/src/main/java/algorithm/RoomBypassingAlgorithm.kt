package algorithm

import RouteMetricRequest
import SonarRequest
import algorithm.geometry.Angle
import algorithm.geometry.AngleData
import algorithm.geometry.Point
import algorithm.geometry.Wall
import objects.Car
import java.util.concurrent.Exchanger
import Logger.log

class RoomBypassingAlgorithm(thisCar: Car, exchanger: Exchanger<IntArray>) : AbstractAlgorithm(thisCar, exchanger) {


    // TODO: set to appropriate values
    override val ATTEMPTS: Int = 5
    override val SMOOTHING = SonarRequest.Smoothing.MEDIAN
    override val WINDOW_SIZE = 3

    //SHOULD BE CALIBRATED BEFORE RUNNING!!!!!!!!!!!
    private val CHARGE_CORRECTION = 0.85

    private val MAX_DISTANCE_TO_WALL_AHEAD = 30         // reached the corner and should turn left
    private val OUTER_CORNER_DISTANCE_THRESHOLD = 60    // reached outer corner
    private val ISOSCALENESS_MIN_DIFF = 5               // have to correct alignment to the wall
    private val ISOSCALENESS_MAX_DIFF = 50              // almost reached outer corner but not yet, and 120 meas. founds far wall
    private val DISTANCE_TO_WALL_UPPER_BOUND = 50       // have to move closer to the parallel wall
    private val DISTANCE_TO_WALL_LOWER_BOUND = 25       // have to move farther to the parallel wall
    private val SPURIOUS_REFLECTION_DIFF = 70           // we're approaching corner and get spurious reflection from two walls on 60 meas. ! Should be before outer corner case !

    private var calibrateAfterRotate = false

    var carX = 0
    var carY = 0
    var carAngle = 0

    override fun getCarState(anglesDistances: Map<Angle, AngleData>): CarState {
        return CarState.WALL
    }

    private fun noParallelWallFound(anglesDistances: Map<Angle, AngleData>, state: CarState): RouteMetricRequest {
        val resultBuilder = RouteMetricRequest.BuilderRouteMetricRequest(IntArray(0), IntArray(0))
        resultBuilder.setDirections(getIntArray(FORWARD))
        resultBuilder.setDistances(getIntArray(10))
        return resultBuilder.build()
    }

    private fun noOrthogonalMeasurementFound(anglesDistances: Map<Angle, AngleData>, state: CarState): RouteMetricRequest {
        val resultBuilder = RouteMetricRequest.BuilderRouteMetricRequest(IntArray(0), IntArray(0))
        resultBuilder.setDirections(getIntArray(FORWARD, RIGHT, FORWARD))
        val wallAngle = calculateAngle(anglesDistances, state)
        resultBuilder.setDistances(getIntArray(15, wallAngle.degs(), 60))
        addWall(-wallAngle)
        calibrateAfterRotate = true
        return resultBuilder.build()
    }

    private fun wallAheadFound(anglesDistances: Map<Angle, AngleData>, state: CarState): RouteMetricRequest {
        val resultBuilder = RouteMetricRequest.BuilderRouteMetricRequest(IntArray(0), IntArray(0))
        resultBuilder.setDirections(getIntArray(LEFT, FORWARD))
        val wallAngle = calculateAngle(anglesDistances, state)
        resultBuilder.setDistances(getIntArray(wallAngle.degs(), 15))
        addWall(wallAngle)
        calibrateAfterRotate = true
        return resultBuilder.build()
    }

    private fun tryAlignParallelToWall(anglesDistances: Map<Angle, AngleData>, state: CarState, average: Double): RouteMetricRequest? {
        for (i in 0..15 step 5) {
            val distRightForward = anglesDistances[Angle(70 + i)]!!
            val distRightBackward = anglesDistances[Angle(110 - i)]!!
            if (distRightBackward.distance > (average * 1.5) || distRightForward.distance > (average * 1.5)) {
                continue
            }

            if (Math.abs(distRightForward.distance - distRightBackward.distance) <= ISOSCALENESS_MIN_DIFF) {
                return null
            }

            log("Flaw in align found, correcting")
            val resultBuilder = RouteMetricRequest.BuilderRouteMetricRequest(IntArray(0), IntArray(0))
            val rotationDirection = if (distRightBackward.distance > distRightForward.distance) LEFT else RIGHT
            resultBuilder.setDirections(getIntArray(rotationDirection))
            resultBuilder.setDistances(getIntArray(Math.min(Math.abs(distRightBackward.distance - distRightForward.distance), 20)))
            return resultBuilder.build()
        }

        return null  // TODO: everything is broken, what to do?
    }

    private fun correctDistanceToParallelWall(anglesDistances: Map<Angle, AngleData>, state: CarState): RouteMetricRequest {
        val resultBuilder = RouteMetricRequest.BuilderRouteMetricRequest(IntArray(0), IntArray(0))

        val rotationDirection = if (anglesDistances[Angle(90)]!!.distance > 50) RIGHT else LEFT

        resultBuilder.setDirections(getIntArray(rotationDirection, FORWARD))
        resultBuilder.setDistances(getIntArray(10, 10))
        return resultBuilder.build()
    }

    private fun moveForward(): RouteMetricRequest {
        val resultBuilder = RouteMetricRequest.BuilderRouteMetricRequest(IntArray(0), IntArray(0))
        resultBuilder.setDirections(getIntArray(FORWARD))
        resultBuilder.setDistances(getIntArray(20))
        return resultBuilder.build()
    }

    private fun addWall(angleWithPrevWall: Angle) {
        log("Adding wall")
        RoomModel.updateWalls()

        val firstWall = RoomModel.walls.first()
        val lastWall = RoomModel.walls.last()
        if (firstWall == lastWall && RoomModel.walls.size > 1) {
            log("Found equal walls, finishing algorithm")
            RoomModel.finished = true
            lastWall.pushBackPoint(firstWall.rawPoints.last())
            lastWall.markAsFinished()
            RoomModel.walls.removeAt(0)
        } else {
            RoomModel.walls.add(Wall(lastWall.wallAngleOX + angleWithPrevWall))
        }
    }

    override fun getCommand(anglesDistances: Map<Angle, AngleData>, state: CarState): RouteMetricRequest? {
        val dist0 = anglesDistances[Angle(0)]!!
        val dist70 = anglesDistances[Angle(70)]!!
        val dist90 = anglesDistances[Angle(90)]!!
        val dist110 = anglesDistances[Angle(110)]!!
        val sonarAngle = carAngle - 90

        if (anglesDistances.filter { it.value.angle.degs() >= 60 && it.value.angle.degs() <= 120 && it.value.distance == -1 }.size != 0) {
            log("Found -1 in angle distances, passing")
            return null
        }

        val average = (anglesDistances.values
                .filter { it.angle.degs() >= 60 && it.angle.degs() <= 120 }
                .filter { it.distance != -1 }
                .sumByDouble { it.distance.toDouble() }) / anglesDistances.values.size
        log("Estimated average = $average")

        if (calibrateAfterRotate) {
            log("Calibrating after rotate")
            val maybeAlignment = tryAlignParallelToWall(anglesDistances, state, average)
            if (maybeAlignment != null) {
                log("Realigning")
                return maybeAlignment
            }
            log("No need to realign")
            calibrateAfterRotate = false
            carAngle = RoomModel.walls.last().wallAngleOX.degs()
        }

        // Check most basic measurements: 60/90/120
        if (dist90.distance == -1 || dist90.distance > OUTER_CORNER_DISTANCE_THRESHOLD) {
            log("No orthogonal measurement found")
            return noOrthogonalMeasurementFound(anglesDistances, state)
        }

        // Add point to room map
        val point = Point(
                x = carX + dist90.distance * Math.cos(degreesToRadian(sonarAngle)),
                y = carY + dist90.distance * Math.sin(degreesToRadian(sonarAngle))
        )
        log("Adding ${point.toString()} to last wall")
        RoomModel.walls.last().pushBackPoint(point)

        // Check if corner reached
        if (dist0.distance != -1 && dist0.distance < MAX_DISTANCE_TO_WALL_AHEAD) {
            log("Wall ahead found")
            return wallAheadFound(anglesDistances, state)
        }

        // Big fail, try to do something safe
        if (dist110.distance == -1 && dist70.distance == -1) {
            log("No parallel wall found")
            return noParallelWallFound(anglesDistances, state)
        }

        // Big fail too
        if (dist110.distance == -1) {
            log("No back measurement found, wtf")
            return moveForward()
        }

        // Approaching inner corner and getting spurious reflection from 2 walls on 60 - just move forward to get closer to corner
        if (dist70.distance - dist0.distance > SPURIOUS_REFLECTION_DIFF) {
            log("Spurious reflection detected, moving forward")
            return moveForward()
        }

        // Approaching outer corner (parallel wall is ending soon, but not yet) - just move forward to get to the end of the wall
        if (dist70.distance == -1 || Math.abs(dist110.distance - dist70.distance) > ISOSCALENESS_MAX_DIFF) {
            log("Aprroaching outer corner, moving forward")
            return moveForward()
        }

        for (i in 70..110 step 10) {
            if (i == 90) {
                continue    // point on 90 was already added
            }

            val curDist = anglesDistances[Angle(i)]!!.distance
            if (curDist > (average * 1.5)) {
                continue
            }
            val point = Point(
                    x = carX + curDist * Math.cos(degreesToRadian(carAngle - i)),
                    y = carY + curDist * Math.sin(degreesToRadian(carAngle - i))
            )
            RoomModel.walls.last().pushBackPoint(point)
        }

        // Try align parallel to wall
        val maybeAlignment = tryAlignParallelToWall(anglesDistances, state, average)
        if (maybeAlignment != null) {
            log("Realigning")
            return maybeAlignment
        }

        // Check if wall is too close or too far
        if (dist90.distance > DISTANCE_TO_WALL_UPPER_BOUND || dist90.distance < DISTANCE_TO_WALL_LOWER_BOUND) {
            log("Flaw in distance to the parallel wall found, correcting")
            return correctDistanceToParallelWall(anglesDistances, state)
        }

        // default case: everything is ok, just move forward
        log("Default case: moving forward")
        return moveForward()
    }

    private fun calculateAngle(anglesDistances: Map<Angle, AngleData>, state: CarState): Angle {
        // TODO: stub here, make proper angle calculation
        return Angle(90)
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
                    route.distances[idx] = (CHARGE_CORRECTION * route.distances[idx]).toInt()
                    carAngle += route.distances[idx]
                }
                RIGHT -> {
                    route.distances[idx] = (CHARGE_CORRECTION * route.distances[idx]).toInt()
                    carAngle -= route.distances[idx]
                }
            }
        }

    }

    private fun degreesToRadian(angle: Int): Double {
        return Math.PI * angle / 180
    }

}