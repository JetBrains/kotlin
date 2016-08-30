package RoomScanner

import CodedInputStream
import CodedOutputStream
import RouteMetricRequest
import SonarRequest
import SonarResponse
import car.client.CarClient
import objects.Car

class CarController(var car: Car) {
    enum class Direction(val id: Int) {
        FORWARD(0),
        BACKWARD(1),
        LEFT(2),
        RIGHT(3);
    }

    var position = Pair(0.0, 0.0)
        private set

    private val WALL_DISTANCE = 50
    private var angle = 0.0

    fun moveTo(to: Pair<Double, Double>) {
        val driveAngle = (Math.toDegrees(Math.atan2(to.first, to.second)) + 360) % 360
        rotateOn(angleDistance(angle, driveAngle))
        drive(Direction.FORWARD, Math.max(0.0, distance(position, to)).toInt() - WALL_DISTANCE)

        position = to
    }

    fun rotateOn(target: Double) {
        val rotateAngle = angleDistance(angle, target)
        val direction = if (rotateAngle > 0) Direction.RIGHT else Direction.LEFT
        drive(direction, rotateAngle.toInt())

        angle = target
    }

    fun scan(angles: IntArray): List<Pair<Double, Double>> {
        val request = SonarRequest.BuilderSonarRequest(angles, IntArray(angles.size, { 1 }), 10, SonarRequest.Smoothing.MEDIAN).build()
        val data = serialize(request.getSizeNoTag(), { i -> request.writeTo(i) })
        val response = CarClient.sendRequest(car, CarClient.Request.SONAR, data).get().responseBodyAsBytes

        val distances = SonarResponse.BuilderSonarResponse(IntArray(0)).parseFrom(CodedInputStream(response)).build().distances

        return distances.mapIndexed { i: Int, distance: Int -> convertToPoint(angles[i].toDouble(), distance.toDouble()) }
    }

    private fun drive(direction: Direction, distance: Int) {
        val request = RouteMetricRequest.BuilderRouteMetricRequest(intArrayOf(distance), intArrayOf(direction.id)).build()
        val data = serialize(request.getSizeNoTag(), { i -> request.writeTo(i) })
        CarClient.sendRequest(car, CarClient.Request.ROUTE_METRIC, data).get()
    }

    private fun convertToPoint(angle: Double, distance: Double): Pair<Double, Double> {
        if (distance <= 0) {
            return Pair(0.0, 0.0)
        }

        val realAngle = Math.toRadians(angle + this.angle)
        return Pair(Math.cos(realAngle) * distance + position.first, Math.sin(realAngle) * distance + position.second)
    }

    private inline fun serialize(size: Int, writeTo: (CodedOutputStream) -> Unit): ByteArray {
        val bytes = ByteArray(size)
        writeTo(CodedOutputStream(bytes))

        return bytes
    }
}
