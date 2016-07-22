import car.client.Client
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.*
import objects.Car
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.concurrent.thread

/**
 * Created by user on 7/6/16.
 */

//хардкод это плохо, но пока так...:)
val carServerPort: Int = 7925
val webServerPort: Int = 7926
val handlerThreadsCount: Int = 100
val getLocationUrl = "/getLocation"
val routeDoneUrl = "/routeDone"
val setRouteUrl = "/route"
val connectUrl = "/connect"

fun main(args: Array<String>) {

    println("car server started")
    val serverCarThread = thread {
        val bossGroup = NioEventLoopGroup(1)
        val workerGroup = NioEventLoopGroup()
        val b = ServerBootstrap()
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel().javaClass)
                .childHandler(car.server.Initializer(handlerThreadsCount))

        try {
            val channel = b.bind(carServerPort).sync().channel()
            channel.closeFuture().sync()
        } catch (e: InterruptedException) {
            println("car server stoped!")
            e.printStackTrace()
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }
    }

    //    val serverWebThread = thread {
    //        val bossGroup = NioEventLoopGroup(1)
    //        val workerGroup = NioEventLoopGroup()
    //        val b = ServerBootstrap()
    //        b.group(bossGroup, workerGroup)
    //                .channel(NioServerSocketChannel().javaClass)
    //                .childHandler(web.server.Initializer(handlerThreadsCount))
    //
    //        try {
    //            val channel = b.bind(webServerPort).sync().channel()
    //            channel.closeFuture().sync()
    //        } catch (e: InterruptedException) {
    //            println("web server stoped!")
    //        } finally {
    //            bossGroup.shutdownGracefully()
    //            workerGroup.shutdownGracefully()
    //        }
    //    }
    val serverWebThread = thread {
        val environment = objects.Environment.instance
        val scanner = Scanner(System.`in`)
        while (scanner.hasNext()) {
            val s = scanner.nextLine()
            if (s.equals("cars", true)) {
                synchronized(environment, {
                    println(environment.map.values)
                })
            } else if (s.equals("pathto", true)) {
                println("print datas. format: [string id] [double distance] [double angle]")
                val data = scanner.nextLine()
                val datas = data.split(" ")
                try {
                    val id = datas[0].toInt()
                    val distance = datas[1].toDouble()
                    val angle = datas[2].toDouble()

                    val car: Car? =
                            synchronized(environment, {
                                environment.map.get(id)
                            })
                    if (car != null) {
                        val wayPoint = RouteRequest.WayPoint.BuilderWayPoint().setDistance(distance).setAngle_delta(angle).build()
                        val route = RouteRequest.BuilderRouteRequest().addWayPoint(wayPoint).build()
                        val requestBytes = ByteArrayOutputStream()
                        route.writeTo(CodedOutputStream(requestBytes))
                        val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, setRouteUrl, Unpooled.copiedBuffer(requestBytes.toByteArray()))
                        request.headers().set(HttpHeaderNames.HOST, car.host)
                        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
                        request.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes())
                        Client.sendRequest(request, car.host, car.port, id)
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    println("incorrect format")
                }
            } else if (s.equals("refloc", true)) {
                val cars = synchronized(environment, { environment.map.values })
                for (car in cars) {
                    val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, getLocationUrl, Unpooled.EMPTY_BUFFER)
                    request.headers().set(HttpHeaderNames.HOST, car.host)
                    request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
                    request.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes())
                    Client.sendRequest(request, car.host, car.port, car.uid)
                }
            }
        }
    }


}