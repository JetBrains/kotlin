import Exceptions.InactiveCarException
import car.client.Client
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.*
import objects.Car
import java.io.ByteArrayOutputStream
import kotlin.concurrent.thread

/**
 * Created by user on 7/6/16.
 */

val timeDeltaToDrop = 600000//time in ms. if car is inactive more than this time, server drop session with car
val carServerPort: Int = 7925
val webServerPort: Int = 7926
val handlerThreadsCount: Int = 100
val getLocationUrl = "/getLocation"
val routeDoneUrl = "/routeDone"
val setRouteUrl = "/route"
val connectUrl = "/connect"

fun main(args: Array<String>) {

    val carServer = getCarServerThread()
//    val webServer = getWebServerThread()
    val carsDestroy = getCarsDestroyThread()
    carServer.start()
    carsDestroy.start()

    //CL user interface
    val helpString = "available commands:\n" +
            "cars - get list of connected cars\n" +
            "route [car_id] - setting a route for car with car id.\n" +
            "refloc - refresh all car locations\n" +
            "stop - exit from this interface and stop all servers\n"
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
                        val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, setRouteUrl, Unpooled.copiedBuffer(requestBytes))
                        request.headers().set(HttpHeaderNames.HOST, car.host)
                        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
                        request.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes())
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
                            println("error in convertion angle or distance to double. try again")
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
                val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, getLocationUrl, Unpooled.EMPTY_BUFFER)
                request.headers().set(HttpHeaderNames.HOST, car.host)
                request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
                request.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes())
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
        } else {
            println("not supported command: $readedString")
            println(helpString)
        }
    }
    carsDestroy.interrupt()
    carServer.interrupt()
}

fun getCarServerThread(): Thread {
    return thread(false, false, null, "carServer", -1, {
        println("car server started")
        val bossGroup = NioEventLoopGroup(1)
        val workerGroup = NioEventLoopGroup()
        val b = ServerBootstrap()
        val initializer = car.server.Initializer(handlerThreadsCount)
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel().javaClass)
                .childHandler(initializer)

        try {
            val channel = b.bind(carServerPort).sync().channel()
            channel.closeFuture().sync()
        } catch (e: InterruptedException) {
            println("car server stoped")
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
            initializer.group.shutdownGracefully()
        }
    })
}

fun getWebServerThread(): Thread {
    return thread(false, false, null, "webServer", -1, {
        println("web server started")
        val bossGroup = NioEventLoopGroup(1)
        val workerGroup = NioEventLoopGroup()
        val b = ServerBootstrap()
        val initializer = web.server.Initializer(handlerThreadsCount)
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel().javaClass)
                .childHandler(initializer)

        try {
            val channel = b.bind(webServerPort).sync().channel()
            channel.closeFuture().sync()
        } catch (e: InterruptedException) {
            println("web server stoped")
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
            initializer.group.shutdownGracefully()
        }
    })
}

//thread, that dropped inactive car. every minute check all connected cars and if car is inactive more, als timeDeltaToDrop then remove this car.
fun getCarsDestroyThread(): Thread {
    return thread(false, false, null, "dropCar", -1, {
        var stoped = false
        while (!stoped) {
            val environment = objects.Environment.instance
            synchronized(environment, {
                val currentTime = System.currentTimeMillis()
                val keysToRemove = mutableListOf<Int>();
                for (keyValue in environment.map) {
                    if ((keyValue.value.lastAction + timeDeltaToDrop) < currentTime) {
                        keysToRemove.add(keyValue.key)
                    }
                }
                for (key in keysToRemove) {
                    environment.map.remove(key)
                }
            })
            try {
                Thread.sleep(60000)
            } catch (e: InterruptedException) {
                println("thread for destroy cars stoped")
                stoped = true
            }
        }
    })
}