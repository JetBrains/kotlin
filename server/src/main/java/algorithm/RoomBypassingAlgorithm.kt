package algorithm

import Logger.log
import RouteMetricRequest
import SonarRequest
import algorithm.geometry.*
import objects.Car
import java.util.concurrent.Exchanger

class RoomBypassingAlgorithm(thisCar: Car, exchanger: Exchanger<IntArray>) : AbstractAlgorithm(thisCar, exchanger) {


    // TODO: set to appropriate values
    override val ATTEMPTS: Int = 5
    override val SMOOTHING = SonarRequest.Smoothing.MEDIAN
    override val WINDOW_SIZE = 3

    //SHOULD BE CALIBRATED BEFORE RUNNING!!!!!!!!!!!
    private val CHARGE_CORRECTION = 0.87//on full charge ok is 0.83 - 0.86

    private val MAX_DISTANCE_TO_WALL_AHEAD = 55         // reached the corner and should turn left
    private val OUTER_CORNER_DISTANCE_THRESHOLD = 90    // reached outer corner
    private val ISOSCALENESS_MIN_DIFF = 5               // have to correct alignment to the wall
    private val ISOSCALENESS_MAX_DIFF = 50              // almost reached outer corner but not yet, and 120 meas. founds far wall
    private val DISTANCE_TO_WALL_UPPER_BOUND = 60       // have to move closer to the parallel wall
    private val DISTANCE_TO_WALL_LOWER_BOUND = 40       // have to move farther to the parallel wall
    private val ECHO_REFLECTION_DIFF = 70           // we're approaching corner and get spurious reflection from two walls on 60 meas. ! Should be before outer corner case !
    private val RANGE_FROM_ZERO_POINT_TO_FINISH_ALG = 50

    private var calibrateAfterRotate = false

    private var isCompleted = false
    private var circleFound = false

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
        resultBuilder.setDistances(getIntArray(20, wallAngle.degs(), 75))
        addWall(-wallAngle)
        carAngle = RoomModel.walls.last().wallAngleOX.degs()
        calibrateAfterRotate = true
        return resultBuilder.build()
    }

    private fun wallAheadFound(anglesDistances: Map<Angle, AngleData>, state: CarState): RouteMetricRequest {
        val resultBuilder = RouteMetricRequest.BuilderRouteMetricRequest(IntArray(0), IntArray(0))
        resultBuilder.setDirections(getIntArray(LEFT, FORWARD))
        val wallAngle = calculateAngle(anglesDistances, state)
        resultBuilder.setDistances(getIntArray(wallAngle.degs(), 15))
        addWall(wallAngle)
        carAngle = RoomModel.walls.last().wallAngleOX.degs()
        calibrateAfterRotate = true
        return resultBuilder.build()
    }

    private fun tryAlignParallelToWall(anglesDistances: Map<Angle, AngleData>, state: CarState, average: Double): RouteMetricRequest? {
        for (i in 0..15 step 5) {
            val distRightForward = anglesDistances[Angle(70 + i)]!!
            val distRightBackward = anglesDistances[Angle(110 - i)]!!
            if (distRightBackward.distance == -1 || distRightForward.distance == -1) {
                log("Found -1 in angle distances, passing")
                continue
            }
            if (distRightBackward.distance > (average * 1.5) || distRightForward.distance > (average * 1.5) ||
                    distRightBackward.distance < (average / 2) || distRightForward.distance < (average / 2)) {
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

        val distToWall = anglesDistances[Angle(90)]!!.distance
        val rotationDirection = if (distToWall > DISTANCE_TO_WALL_UPPER_BOUND) RIGHT else LEFT
        val backRotationDirection = if (rotationDirection == RIGHT) LEFT else RIGHT
        resultBuilder.setDirections(getIntArray(rotationDirection, FORWARD, backRotationDirection))
        val rangeToCorridor = if (distToWall > DISTANCE_TO_WALL_UPPER_BOUND) {
            distToWall - DISTANCE_TO_WALL_UPPER_BOUND
        } else {
            DISTANCE_TO_WALL_LOWER_BOUND - distToWall
        }
        resultBuilder.setDistances(getIntArray(2 * rangeToCorridor + 5, 20, (1.5 * rangeToCorridor).toInt()))
        return resultBuilder.build()
    }

    private fun moveForward(distToWall: Int): RouteMetricRequest {
        val resultBuilder = RouteMetricRequest.BuilderRouteMetricRequest(IntArray(0), IntArray(0))
        resultBuilder.setDirections(getIntArray(FORWARD))
        resultBuilder.setDistances(getIntArray(Math.max(distToWall / 4, 20)))
        return resultBuilder.build()
    }

    private fun addWall(angleWithPrevWall: Angle) {
        log("Adding wall")
        RoomModel.updateWalls()

        val firstWall = RoomModel.walls.first()
        val lastWall = RoomModel.walls.last()
//        if (firstWall == lastWall && RoomModel.walls.size > 1) {
        if (circleFound) {
            log("Found circle, finishing algorithm")
            isCompleted = true
            RoomModel.finished = true
            val intersectionPoint = firstWall.rawPoints.last()
            intersectionPoint.y = lastWall.rawPoints.first().y
            lastWall.pushBackPoint(intersectionPoint)
            lastWall.markAsFinished()
            RoomModel.walls.removeAt(0)
        } else {
            RoomModel.walls.add(Wall(lastWall.wallAngleOX + angleWithPrevWall))
        }
    }

    override fun getCommand(anglesDistances: Map<Angle, AngleData>, state: CarState): RouteMetricRequest? {
        val dist0 = anglesDistances[Angle(0)]!!
        val dist70 = anglesDistances[Angle(70)]!!
        val dist80 = anglesDistances[Angle(80)]!!
        val dist90 = anglesDistances[Angle(90)]!!
        val dist100 = anglesDistances[Angle(100)]!!
        val dist110 = anglesDistances[Angle(110)]!!
        val sonarAngle = carAngle - 90

        if (anglesDistances.filter {
            it.value.angle.degs() >= 60
                    && it.value.angle.degs() <= 120
                    && it.value.distance == -1
        }.size > anglesDistances.size / 2) {
            log("Found to many -1 in angle distances, passing")
            //todo Теоретически такая ситуация может быть валидной, если сразу после внутреннего угла идёт внешний
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
            return moveForward(dist0.distance)
        }

        for (i in 70..110 step 10) {
            if (i == 90) {
                continue    // point on 90 was already added
            }

            val curDist = anglesDistances[Angle(i)]!!.distance
            if (curDist > (average * 1.5) || curDist < (average / 2)) {
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

        // Approaching inner corner and getting spurious reflection from 2 walls on 60;
        // Just move forward to get closer to the corner;
        if (dist70.distance - dist0.distance > ECHO_REFLECTION_DIFF) {
            log("Spurious reflection detected, moving forward")
            return moveForward(dist0.distance)
        }

        // Approaching outer corner (parallel wall is ending soon, but not yet);
        // Just move forward to get to the end of the wall
        if (dist80.distance == -1 || Math.abs(dist100.distance - dist80.distance) > ISOSCALENESS_MAX_DIFF) {
            log("Aprroaching outer corner, moving forward")
            return moveForward(dist0.distance)
        }

        // default case: everything is ok, just move forward
        log("Default case: moving forward")
        return moveForward(dist0.distance)
    }

    private fun calculateAngle(anglesDistances: Map<Angle, AngleData>, state: CarState): Angle {
        // TODO: stub here, make proper angle calculation
        return Angle(90)
    }

    private fun getIntArray(vararg args: Int): IntArray {
        return args
    }

    override fun isCompleted(): Boolean {
        return isCompleted
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
//                    carAngle += route.distances[idx]
                }
                RIGHT -> {
                    route.distances[idx] = (CHARGE_CORRECTION * route.distances[idx]).toInt()
//                    carAngle -= route.distances[idx]
                }
            }
        }
        if (Math.round(Math.cos(Angle(carAngle).rads())).toInt() == 1 && carAngle != 0
                && Vector(carX.toDouble(), carY.toDouble()).length() < RANGE_FROM_ZERO_POINT_TO_FINISH_ALG) {
            circleFound = true
        }

    }

    private fun degreesToRadian(angle: Int): Double {
        return Math.PI * angle / 180
    }

}