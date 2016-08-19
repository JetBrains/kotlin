import Exceptions.InactiveCarException
import car.client.Client
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import objects.Car

/**
 * Created by user on 8/18/16.
 */
class DebugClInterface {

    fun run() {

        val helpString = "available commands:\n" +
                "cars - get list of connected cars\n" +
                "route [car_id] - setting a route for car with car id.\n" +
                "refloc - refresh all car locations\n" +
                "dbinfo [car_id] [type] - refresh all car locations\n." +
                "type is string name of value or int value. available values: MEMORYSTATS - 0"
        //        "stop - exit from this interface and stop all servers\n"//todo sometimes server not completed. some threads dont stop
        println(helpString)
        val routeRegex = Regex("route [0-9]{1,10}")
        val environment = objects.Environment.instance
        while (true) {
            val readedString = readLine()
            if (readedString == null) {
                break
            }
            if (readedString.equals("cars", true)) {
                synchronized(environment, {
                    println(environment.map.values)
                })
            } else if (routeRegex.matches(readedString)) {

                val id: Int
                try {
                    id = readedString.split(" ")[1].toInt()
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                    println("error in converting id to int type")
                    println(helpString)
                    continue
                }
                val car: Car? =
                        synchronized(environment, {
                            environment.map.get(id)
                        })
                if (car != null) {
                    println("print way points in polar coordinate als [distance] [rotation angle] in metres and degrees. after enter all points print \"done\". for reset route print \"reset\"")
                    println("e.g. for move from (x,y) to (x+1,y) and back to (x,y) u need enter: 1 0[press enter] 1 180[press enter] done")

                    val distances = arrayListOf<Int>()
                    val angles = arrayListOf<Int>()
                    while (true) {
                        val wayPointInputString = readLine()!!
                        if (wayPointInputString.equals("reset", true)) {
                            break
                        } else if (wayPointInputString.equals("done", true)) {
                            val routeBuilder = RouteRequest.BuilderRouteRequest(distances.toIntArray(), angles.toIntArray())
                            val requestObject = routeBuilder.build()
                            val requestBytes = ByteArray(requestObject.getSizeNoTag())
                            requestObject.writeTo(CodedOutputStream(requestBytes))
                            val request = getDefaultHttpRequest(car.host, setRouteUrl, requestBytes)
                            try {
                                Client.sendRequest(request, car.host, car.port, id)
                            } catch (e: InactiveCarException) {
                                synchronized(environment, {
                                    environment.map.remove(id)
                                })
                            }
                            break
                        } else {
                            val wayPointData = wayPointInputString.split(" ")
                            val distance: Int
                            val angle: Int
                            try {
                                distance = wayPointData[0].toInt()
                                angle = wayPointData[1].toInt()
                            } catch (e: NumberFormatException) {
                                println("error in convertion angle or distance to int. try again")
                                continue
                            } catch (e: IndexOutOfBoundsException) {
                                println("format error, u must print two number separated by spaces. Try again")
                                continue
                            }
                            distances.add(distance)
                            angles.add(angle)
                        }
                    }
                } else {
                    println("car with id=$id not found")
                }
            } else if (readedString.equals("refloc", true)) {
                val cars = synchronized(environment, { environment.map.values })
                val inactiveCarUids = mutableListOf<Int>()
                for (car in cars) {
                    val request = getDefaultHttpRequest(car.host, getLocationUrl, ByteArray(0))
                    try {
                        Client.sendRequest(request, car.host, car.port, car.uid)
                    } catch (e: InactiveCarException) {
                        inactiveCarUids.add(car.uid)
                    }
                    println("ref loc done")
                }
                synchronized(environment, {
                    for (id in inactiveCarUids) {
                        environment.map.remove(id)
                    }
                })
            } else if (readedString.equals("stop")) {
                break
            } else if (readedString.contains("dbinfo")) {
                val params = readedString.split(" ")
                if (!params[0].equals("dbinfo")) {
                    println("not supported command: $readedString")
                    println(helpString)
                    continue
                }
                if (params.size != 3) {
                    println("incorrect args of command dbinfo.")
                    println(helpString)
                    continue
                }
                val id: Int
                try {
                    id = params[1].toInt()
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                    println("error in converting id to int type")
                    println(helpString)
                    continue
                }
                val car: Car? =
                        synchronized(environment, {
                            environment.map.get(id)
                        })
                if (car == null) {
                    println("car with id=$id not found")
                    continue
                }
                val paramType = params[2]//maybe string or int
                val type: DebugRequest.Type
                try {
                    type = DebugRequest.Type.fromIntToType(paramType.toInt())
                } catch (e: NumberFormatException) {
                    try {
                        type = DebugRequest.Type.valueOf(paramType)
                    } catch (e: IllegalArgumentException) {
                        type = DebugRequest.Type.Unexpected
                    }
                }
                if (type == DebugRequest.Type.Unexpected) {
                    println("type with name/id $paramType not found")
                    continue
                }
                val requestObject = DebugRequest.BuilderDebugRequest(type).build()
                val bytes = ByteArray(requestObject.getSizeNoTag())
                requestObject.writeTo(CodedOutputStream(bytes))
                val request = getDefaultHttpRequest(car.host, debugMemoryUrl, bytes)
                try {
                    Client.sendRequest(request, car.host, car.port, car.uid)
                } catch (e: InactiveCarException) {
                    println("this car is inactive")
                }
            } else {
                println("not supported command: $readedString")
                println(helpString)
            }
        }
    }

    fun getDefaultHttpRequest(host: String, url: String, bytes: ByteArray): DefaultFullHttpRequest {
        val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, url, Unpooled.copiedBuffer(bytes));
        request.headers().set(HttpHeaderNames.HOST, host)
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
        request.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes())
        return request
    }

}