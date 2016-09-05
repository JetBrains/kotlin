package clInterface

import clInterface.executor.*

object DebugClInterface {

    private val HELP_STRING = "available handlers:\n" +
            "cars - get list of connected cars\n" +
            "route [car_id] - setting a route for net.car with net.car id.\n" +
            "sonar [car_id] - get sonar data\n" +
            "dbinfo [car_id] [type] - refresh all net.car locations\n" +
            "type is int value from: MEMORY_STATS - 0, SONAR_STATS - 1\n" +
            "alg [count] - run algorithm. Make [count] iteration\n" +
            "stop - exit from this interface and stop all threads\n"

    private val algorithmExecutor = Algorithm()
    private val executors = mapOf(
            Pair("cars", CarsInformation()),
            Pair("route", RouteMetric()),
            Pair("sonar", Sonar()),
            Pair("dbinfo", DebugInformation()),
            Pair("alg", algorithmExecutor),
            Pair("explore", Explore())
    )

    fun run() {
        println(HELP_STRING)
        while (true) {
            val readString = readLine()
            if (readString == null || readString.equals("stop")) {
                algorithmExecutor.interruptAlgorithmThread()
                break
            }
            if (readString.equals("")) {
                continue
            }
            try {
                executeCommand(readString)
            } catch (exception: Exception) {
                exception.printStackTrace()
                println("Fail to execute command[$readString]: $exception")
                println(HELP_STRING)
            }
        }

    }

    private fun printNotSupportedCommand(command: String) {
        println("Incorrect command: $command")
        println(HELP_STRING)
    }

    private fun executeCommand(readString: String) {
        val command = readString.split(" ")[0].toLowerCase()
        val executor = executors[command]
        if (executor == null) {
            printNotSupportedCommand(command)
            return
        }
        executor.execute(readString)
    }
}
