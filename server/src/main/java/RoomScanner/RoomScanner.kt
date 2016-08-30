package RoomScanner

class RoomScanner(val controller: CarController): Thread() {
    private val points = mutableListOf<Pair<Double, Double>>()

    override fun run() {
        while (true) {
            scan()
            println("[${points.joinToString { "[${it.first}, ${it.second}]" }}}]")
        }
    }

    fun scan() {
        val horizon = IntArray(180 / 5, { it * 5 })
        val iterationPoints = mutableListOf<Pair<Double, Double>>()

        controller.rotateOn(0.0)
        iterationPoints.addAll(controller.scan(horizon))
        controller.rotateOn(180.0)
        iterationPoints.addAll(controller.scan(horizon))

        points.addAll(iterationPoints)

        val target = iterationPoints.filter { it.first > 0 && it.second > 0 }.maxBy { distance(controller.position, it) }
        target ?: return

        controller.moveTo(target)
    }

}
