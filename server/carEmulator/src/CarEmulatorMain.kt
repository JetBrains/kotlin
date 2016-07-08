import client.Client
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.*
import proto.car.ConnectP
import proto.car.RouteDoneP
import server.Initializer
import java.util.*
import kotlin.concurrent.thread

/**
 * Created by user on 7/6/16.
 */


val serverHost = "127.0.0.1"
val serverPort = 7925
val carHost = "127.0.0.1"

val getLocationUrl = "getLocation"
val routeDoneUrl = "routeDone"
val setRouteUrl = "route"
val connectUrl = "connect"

fun main(args: Array<String>) {
    val port = Random().nextInt(10000) + 10000
    println("car started on port $port")

    val serverCarThread = thread {
        val bossGroup = NioEventLoopGroup(1)
        val workerGroup = NioEventLoopGroup()
        val b = ServerBootstrap()
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel().javaClass)
                .childHandler(Initializer())

        try {
            val channel = b.bind(port).sync().channel()
            channel.closeFuture().sync()
        } catch (e: InterruptedException) {
            println("car server stoped!")
            e.printStackTrace()
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }
    }
    //connect
    val connect = ConnectP.ConnectionRequest.newBuilder().setIp(carHost).setPort(port).build()
    val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, connectUrl, Unpooled.copiedBuffer(connect.toByteArray()))
    request.headers().set(HttpHeaderNames.HOST, serverHost)
    request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
    request.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes())

    Client.sendRequest(request, serverHost, serverPort)

    val car = ThisCar.instance
    var lastTime: Long = System.currentTimeMillis()
    while (true) {
        Thread.sleep(50)
        val deltaTimeMs = (System.currentTimeMillis() - lastTime)
        if (!car.pathDone) {
            println(car)
        }
        var pathDone = car.move(deltaTimeMs.toDouble() / 1000)
        if (pathDone) {
            val routeDone = RouteDoneP.RouteDone.newBuilder().setUid(car.id).build()
            val pathDoneRequest = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, setRouteUrl, Unpooled.copiedBuffer(routeDone.toByteArray()))
            pathDoneRequest.headers().set(HttpHeaderNames.HOST, serverHost)
            pathDoneRequest.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
            pathDoneRequest.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, pathDoneRequest.content().readableBytes())
            Client.sendRequest(pathDoneRequest, serverHost, serverPort)
        }
        lastTime = System.currentTimeMillis()
    }
}
