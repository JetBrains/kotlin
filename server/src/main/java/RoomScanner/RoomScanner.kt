package RoomScanner

class RoomScanner(val controller: CarController) {
    private val points = mutableListOf<Pair<Double, Double>>()

    fun run() {
        while (true) {
            scan()
            println("[${points.joinToString { "[${it.first}, ${it.second}]" }}}]")
        }
    }

    fun scan() {
        val horizon = IntArray(180 / 5, { it * 5 })

        controller.rotateOn(0.0)
        val iterationPoints = controller.scan(horizon)

        controller.rotateOn(180.0)
        iterationPoints.addAll(controller.scan(horizon))

        points.addAll(iterationPoints)

        val target = iterationPoints.maxBy { distance(controller.position, it) }
        target ?: return

        controller.moveTo(target)
    }

}
