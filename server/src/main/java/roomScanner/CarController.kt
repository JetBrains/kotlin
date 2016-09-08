package roomScanner

import CodedInputStream
import RouteMetricRequest
import SonarRequest
import SonarResponse
import net.car.client.Client
import objects.CarReal

class CarController(var car: CarReal) {
    enum class Direction(val id: Int) {
        FORWARD(0),
        BACKWARD(1),
        LEFT(2),
        RIGHT(3);
    }

    //todo use x,y,angle from car object. x,y in sm, angle in degrees
    var position = Pair(0.0, 0.0)
        private set
    private var angle = 0.0

    private val CHARGE_CORRECTION = 0.97
    private val MAX_ANGLE = 360.0
    private val SCAN_STEP = 5.0
    private val MIN_ROTATION = 10
    private val MAX_VALID_DISTANCE = 100.0

    fun moveTo(to: Pair<Double, Double>, distance: Double) {
        val driveAngle = (Math.toDegrees(Math.atan2(to.second, to.first)) + MAX_ANGLE) % MAX_ANGLE
        rotateOn(driveAngle)
        drive(Direction.FORWARD, distance.toInt())

        position = Pair(position.first + distance * Math.cos(Math.toRadians(driveAngle)),
                position.second + distance * Math.sin(Math.toRadians(driveAngle)))
    }

    fun rotateOn(target: Double) {
        val rotateAngle = angleDistance(angle, target)
        val direction = if (rotateAngle > 0) Direction.LEFT else Direction.RIGHT
        drive(direction, Math.abs(rotateAngle.toInt()))

        angle = target
    }

    fun rotateLeftWithCorrection(angle: Double) {
        val horizon = horizon()
        val before = scan(horizon)
        drive(Direction.LEFT, angle.toInt())
        var after = scan(horizon)

        val distance = fun(first: Double, second: Double): Double = Math.max(when {
            first == second -> 0.0
            first == -1.0 -> second
            second == -1.0 -> first
            else -> Math.max(Math.abs(first - second).toDouble(), 100.0)
        }, MAX_VALID_DISTANCE)

        var realAngle = (maxSuffix(before, after, distance) + 1) * SCAN_STEP
        while (realAngle != angle) {
            val direction = if (realAngle > angle) Direction.RIGHT else Direction.LEFT
            drive(direction, Math.min(MIN_ROTATION, Math.abs(realAngle - angle).toInt()))

            after = scan(horizon)
            realAngle = maxSuffix(before, after, distance) * SCAN_STEP
        }

        this.angle = realAngle
    }

    fun scan(angles: IntArray): List<Double> {
        val request = SonarRequest.BuilderSonarRequest(angles, IntArray(angles.size, { 1 }), 1, SonarRequest.Smoothing.NONE).build()

        val data = serialize(request.getSizeNoTag(), { request.writeTo(it) })

        val response = car.carConnection.sendRequest(Client.Request.SONAR, data).get().responseBodyAsBytes

        val result = SonarResponse.BuilderSonarResponse(IntArray(0)).parseFrom(CodedInputStream(response)).build().distances.map { it.toDouble() }
        return result
    }

    fun convertToPoint(angle: Double, distance: Double): Pair<Double, Double> {
        if (distance <= 0) {
            return Pair(0.0, 0.0)
        }

        val realAngle = Math.toRadians(angle + this.angle)
        return Pair(Math.cos(realAngle) * distance + position.first, Math.sin(realAngle) * distance + position.second)
    }

    fun drive(direction: Direction, distance: Int) {
        val request = RouteMetricRequest.BuilderRouteMetricRequest(intArrayOf((distance * CHARGE_CORRECTION).toInt()), intArrayOf(direction.id)).build()
        val data = serialize(request.getSizeNoTag(), { i -> request.writeTo(i) })
        car.carConnection.sendRequest(Client.Request.ROUTE_METRIC, data).get()
    }
}
