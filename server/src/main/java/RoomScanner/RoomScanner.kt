package RoomScanner

class RoomScanner(val controller: CarController) : Thread() {
    private val points = mutableListOf<Pair<Double, Double>>()
    private val VALID_MEASURE = 150
    private val GHOST_LEVEL = 50

    override fun run() {
        println("Car connected ${controller.car.carConnection.host}")
        for (i in 1..100) {
            step()
            println(plot(points))
        }

    }

    private fun step() {
        val iterationPoints = scan().filter { it.first > 0 || it.second > 0 }
        points.addAll(iterationPoints.filter { distance(controller.position, it) <= VALID_MEASURE })

        val target = iterationPoints.sortedBy { distance(controller.position, it) }.takeLast(GHOST_LEVEL)[(Math.random() * GHOST_LEVEL).toInt()]
        controller.moveTo(target, Math.min(distance(controller.position, target), 100.0))
        controller.rotateOn(0.0)
    }

    private fun scan(): MutableList<Pair<Double, Double>> {
        val horizon = horizon()

        val dots = mutableListOf<Pair<Double, Double>>()
        for (i in arrayOf(0.0, 90.0, 180.0, 270.0)) {
            dots.addAll(controller.scan(horizon).mapIndexed { i: Int, d: Double -> controller.convertToPoint((i * 5 - 180).toDouble(), d) })
            controller.drive(CarController.Direction.FORWARD, 10)
            controller.drive(CarController.Direction.BACKWARD, 10)

            controller.rotateLeftWithCorrection(90.0)
        }

        return dots
    }

    private fun plot(points: MutableList<Pair<Double, Double>>) =
            "x <- c(${points.joinToString { it.first.toInt().toString() }}) \ny <- c(${points.joinToString { it.second.toInt().toString() }})"
}
