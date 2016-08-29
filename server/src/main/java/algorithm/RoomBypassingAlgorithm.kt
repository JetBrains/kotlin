package algorithm

import RouteMetricRequest
import algorithm.geometry.Angle
import algorithm.geometry.AngleData
import algorithm.geometry.Point
import algorithm.geometry.Wall
import objects.Car
import java.util.concurrent.Exchanger

class RoomBypassingAlgorithm(thisCar: Car, exchanger: Exchanger<IntArray>) : AbstractAlgorithm(thisCar, exchanger) {

    // TODO: set to appropriate values
    override val THRESHOLD = 0
    override val ATTEMPTS: Int = 3
    override val SMOOTHING = SonarRequest.Smoothing.MEDIAN

    private val MAX_DISTANCE_TO_WALL_AHEAD = 60
    private val ISOSCALENESS_THRESHOLD = 10
    private val DISTANCE_TO_WALL_UPPER_BOUND = 50
    private val DISTANCE_TO_WALL_LOWER_BOUND = 25

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
        resultBuilder.setDirections(getIntArray(RIGHT))
        resultBuilder.setDistances(getIntArray(10))
        return resultBuilder.build()
    }

    private fun wallAheadFound(anglesDistances: Map<Angle, AngleData>, state: CarState): RouteMetricRequest {
        val resultBuilder = RouteMetricRequest.BuilderRouteMetricRequest(IntArray(0), IntArray(0))
        resultBuilder.setDirections(getIntArray(LEFT))
        resultBuilder.setDistances(getIntArray(calculateAngle(anglesDistances, state).degs()))
        addWall()
        return resultBuilder.build()
    }

    private fun alignParallelToWall(anglesDistances: Map<Angle, AngleData>, state: CarState): RouteMetricRequest {
        val resultBuilder = RouteMetricRequest.BuilderRouteMetricRequest(IntArray(0), IntArray(0))

        // we're sure that we have 120 and 60 degs measurements as they were checked before
        val rotationDirection = if (anglesDistances[Angle(120)]!!.distance > anglesDistances[Angle(60)]!!.distance) LEFT else RIGHT

        resultBuilder.setDirections(getIntArray(rotationDirection, FORWARD))
        resultBuilder.setDistances(getIntArray(10, 7))//todo calibrate
        return resultBuilder.build()
    }

    private fun correctDistanceToParallelWall(anglesDistances: Map<Angle, AngleData>, state: CarState): RouteMetricRequest {
        val resultBuilder = RouteMetricRequest.BuilderRouteMetricRequest(IntArray(0), IntArray(0))

        val rotationDirection = if (anglesDistances[Angle(90)]!!.distance > 50) RIGHT else LEFT

        resultBuilder.setDirections(getIntArray(rotationDirection, FORWARD))
        resultBuilder.setDistances(getIntArray(10, 10))
        return resultBuilder.build()
    }

    private fun addWall() {
        RoomModel.updateWalls()

        if (RoomModel.walls.last() == RoomModel.walls.first() && RoomModel.walls.size > 1) {
            println("YEAH!")
            RoomModel.finished = true
            RoomModel.walls.removeAt(RoomModel.walls.size - 1)
        }
        RoomModel.walls.add(Wall())
    }

    override fun getCommand(anglesDistances: Map<Angle, AngleData>, state: CarState): RouteMetricRequest {
        val dist0 = anglesDistances[Angle(0)]
        val dist60 = anglesDistances[Angle(60)]
        val dist90 = anglesDistances[Angle(90)]
        val dist120 = anglesDistances[Angle(120)]
        val sonarAngle = carAngle - 90

        // Check most basic measurements: 60/90/120
        if (dist90 == null) {
            return noOrthogonalMeasurementFound(anglesDistances, state)
        }

        if (dist120 == null || dist60 == null) {
            return noParallelWallFound(anglesDistances, state)
        }

        // Add point to room map
        val point = Point(
                x = carX + dist90.distance * Math.cos(degreesToRadian(sonarAngle)),
                y = carY + dist90.distance * Math.sin(degreesToRadian(sonarAngle))
        )
        RoomModel.walls.last().addPoint(point)

        // Check if corner reached
        if (dist0 != null && dist0.distance < MAX_DISTANCE_TO_WALL_AHEAD) {
            return wallAheadFound(anglesDistances, state)
        }

        // Checks for paralleleness

        // Check if wall is not parallel
        val diff = Math.abs(dist120.distance - dist60.distance)
        if (diff > ISOSCALENESS_THRESHOLD) {
            return alignParallelToWall(anglesDistances, state)
        }

        // Check if wall is too close or too far
        if (dist90.distance > 50 || dist90.distance < 25) {
            return correctDistanceToParallelWall(anglesDistances, state)
        }

        // default case: everything is ok, just move forward
        val resultBuilder = RouteMetricRequest.BuilderRouteMetricRequest(IntArray(0), IntArray(0))
        resultBuilder.setDirections(getIntArray(FORWARD))
        resultBuilder.setDistances(getIntArray(20))
        return resultBuilder.build()
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