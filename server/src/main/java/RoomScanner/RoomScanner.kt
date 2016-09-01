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
        val iterationPoints = scan()
        points.addAll(iterationPoints.filter { it.first > 0 || it.second > 0 })

        val target = iterationPoints.filter { it.first > 0 && it.second > 0 }.maxBy { distance(controller.position, it) }
        target ?: return

        controller.moveTo(target)
    }

    private fun scan(): MutableList<Pair<Double, Double>> {
        val horizon = IntArray(180 / 5, { it * 5 })

        val distance = fun(first: Double, second: Double): Double = when {
            first == second -> 0.0
            first == -1.0 -> second
            second == -1.0 -> first
            else -> Math.abs(first - second)
        }

        val metrics = mutableListOf<List<Double>>()
        for (i in intArrayOf(0, 180)) {
            controller.rotateOn(i.toDouble())
            metrics.add(controller.scan(horizon))
        }

        val points = merge(metrics.reversed(), distance).reversed()
        return points.mapIndexed { i: Int, d: Double -> controller.convertToPoint((i * 5).toDouble(), d) }.toMutableList()
    }

    private fun plot(points: MutableList<Pair<Double, Double>>) =
            "x <- c(${points.joinToString { it.first.toInt().toString() }}) \n y <- c(${points.joinToString { it.second.toInt().toString() }})"
}
