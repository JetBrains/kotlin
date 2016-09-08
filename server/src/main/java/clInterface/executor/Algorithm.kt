package clInterface.executor

import algorithm.AlgorithmThread
import algorithm.RoomBypassingAlgorithm
import objects.Environment

class Algorithm : CommandExecutor {

    private val algorithmThread = AlgorithmThread()

    override fun execute(command: String) {
        val params = command.split(" ")
        val count =
                if (params.size == 2) try {
                    params[1].toInt()
                } catch (e: Exception) {
                    1
                } else 1
        if (algorithmThread.algorithmImpl == null) {
            algorithmThread.algorithmImpl = RoomBypassingAlgorithm(Environment.map.values.last())
            algorithmThread.start()
        }
        algorithmThread.setCount(count)
    }

    fun interruptAlgorithmThread() {
        algorithmThread.setCount(0)
        algorithmThread.interrupt()
    }
}
