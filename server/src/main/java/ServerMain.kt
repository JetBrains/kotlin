import Exceptions.InactiveCarException
import car.client.Client
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.*
import objects.Car
import java.io.ByteArrayOutputStream
import java.util.*
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
val debugMemoryUrl = "/debug/memory"

fun main(args: Array<String>) {

    val carServer = getCarServerThread()
//    val webServer = getWebServerThread()
    val carsDestroy = getCarsDestroyThread()
    carServer.start()
    carsDestroy.start()

    //CL user interface
    DebugClInterface().run()

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
                    //todo this car is MAYBE disconnect. need ping this car and if dont have answer - drop
//                    environment.map.remove(key)
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