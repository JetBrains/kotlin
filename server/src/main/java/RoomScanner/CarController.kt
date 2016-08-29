package RoomScanner

import objects.Car

class CarController(var car: Car) {
    enum class Direction(val id: Int) {
        FORWARD(0),
        BACKWARD(1),
        LEFT(2),
        RIGHT(3);
    }

    val WALL_DISTANCE = 50

    var position = Pair(0.0, 0.0)
        private set

    private var angle = 0.0

    fun moveTo(to: Pair<Double, Double>) {
        val oldAngle = angle
        rotateOn(estimateAngle(position, to))
        drive(Direction.FORWARD, Math.max(0.0, distance(position, to)).toInt() - WALL_DISTANCE)
        rotateOn(oldAngle)
    }


    fun rotateOn(target: Double) {
        val rotateAngle = angleDistance(angle, target)
        val direction = if (rotateAngle > 0) Direction.RIGHT else Direction.LEFT
        drive(direction, rotateAngle.toInt())

        angle = rotateAngle
    }

    private fun drive(direction: Direction, distance: Int) {
        when (direction) {
            Direction.BACKWARD -> {}
            Direction.FORWARD -> {}
            Direction.LEFT -> {}
            Direction.RIGHT -> {}
        }
    }

    fun scan(angles: IntArray): MutableList<Pair<Double, Double>> {
        throw UnsupportedOperationException()
    }


}