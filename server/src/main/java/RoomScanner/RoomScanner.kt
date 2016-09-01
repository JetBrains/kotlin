package RoomScanner

class RoomScanner(val controller: CarController) : Thread() {
    private val points = mutableListOf<Pair<Double, Double>>()

    override fun run() {
        println("Car connected ${controller.car.host}")
        while (true) {
            step()
            println(plot(points))
        }
    }

    private fun step() {
        val iterationPoints = scan().filter { it.first > 0 || it.second > 0}
        points.addAll(iterationPoints)

        val target = iterationPoints.maxBy { distance(controller.position, it) }
        target ?: return

        controller.moveTo(target, 50.0)
        controller.rotateOn(0.0)
    }

    private fun scan(): MutableList<Pair<Double, Double>> {
        val horizon = IntArray(180 / 5, { it * 5 })
        horizon.reverse()

        val dots = mutableListOf<Pair<Double, Double>>()
        for (i in arrayOf(0.0, 90.0, 180.0, 270.0)) {
            controller.rotateOn(i)
            dots.addAll(controller.scan(horizon).mapIndexed { i: Int, d: Double -> controller.convertToPoint((i * 5 - 180).toDouble(), d) })
        }

        controller.rotateOn(0.0)

        return dots
    }

    private fun plot(points: MutableList<Pair<Double, Double>>) =
            "x <- c(${points.joinToString { it.first.toInt().toString() }}) \ny <- c(${points.joinToString { it.second.toInt().toString() }})"
}
