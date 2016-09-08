package clInterface.executor

import CodedOutputStream
import RouteMetricRequest
import net.car.client.Client
import objects.Car
import objects.CarReal
import objects.Environment
import java.net.ConnectException

class RouteMetric : CommandExecutor {

    private val ROUTE_REGEX = Regex("route [0-9]{1,10}")
    private val HELP_STRING = "print way points als [distance/degrees] [direction] in sm and degrees." +
            "after enter all points print \"done\". for reset route print \"reset\". available directions:" +
            "0 - FORWARD, 1 - BACKWARD, 2 - LEFT, 3 - RIGHT"

    override fun execute(command: String) {
        if (!ROUTE_REGEX.matches(command)) {
            println("incorrect args of route command")
            return
        }
        val id: Int
        try {
            id = command.split(" ")[1].toInt()
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            println("error in converting id to int type")
            return
        }
        val car: Car? =
                synchronized(Environment, {
                    Environment.map[id]
                })
        if (car == null) {
            println("car with id=$id not found")
            return
        }
        val routeMessage = getRouteMessage() ?: return
        val requestBytes = ByteArray(routeMessage.getSizeNoTag())
        routeMessage.writeTo(CodedOutputStream(requestBytes))
        try {
            routeMessage.distances.forEachIndexed { idx, distance ->
                val direction = roomScanner.CarController.Direction.values()
                        .filter { it.id == routeMessage.directions[idx] }.first()
                car.moveCar(distance, direction).get()
            }
        } catch (e: ConnectException) {
            synchronized(Environment, {
                Environment.map.remove(id)
            })
        }
    }

    private fun getRouteMessage(): RouteMetricRequest? {
        println(HELP_STRING)
        val distances = arrayListOf<Int>()
        val directions = arrayListOf<Int>()
        while (true) {
            val readLine = readLine()!!.toLowerCase()
            when (readLine) {
                "reset" -> return null
                "done" -> {
                    val routeBuilder = RouteMetricRequest.BuilderRouteMetricRequest(distances.toIntArray(), directions.toIntArray())
                    return routeBuilder.build()
                }
            }
            val wayPointData = readLine.split(" ")
            val distance: Int
            val direction: Int
            try {
                distance = wayPointData[0].toInt()
                direction = wayPointData[1].toInt()
            } catch (e: NumberFormatException) {
                println("error in converting distance or direction to int. try again")
                continue
            } catch (e: IndexOutOfBoundsException) {
                println("format error, you must print two number separated by spaces. Try again")
                continue
            }
            if (direction != 0 && direction != 1 && direction != 2 && direction != 3) {
                println("direction $direction don't supported!")
                println(HELP_STRING)
            }
            distances.add(distance)
            directions.add(direction)
        }
    }


}