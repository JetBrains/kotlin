package web.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import kotlin.concurrent.thread

object Server {
    enum class ServerMode {
        IDLE,
        MANUAL_MODE,
        PERIMETER_BUILDING,
        PERIMETER_DEBUG;

        companion object {
            fun fromProtoMode(mode: ModeChange.Mode): ServerMode {
                return when (mode) {
                    ModeChange.Mode.ManualControl -> MANUAL_MODE
                    ModeChange.Mode.PerimeterBuilding -> PERIMETER_BUILDING
                    ModeChange.Mode.PerimeterDebug -> PERIMETER_DEBUG
                    else -> throw IllegalArgumentException("Illegal argument when parsing ServerMode from Protobuf Mode")
                }
            }
        }
    }

    private val handlerThreadsCount: Int = 10
    var serverMode = ServerMode.IDLE

    fun changeMode(newMode: ServerMode) {
        println("Changing mode from ${serverMode.toString()} to ${newMode.toString()}")
        serverMode = newMode
    }

    fun getWebServerThread(webServerPort: Int): Thread {
        return thread(false, false, null, "webServer", -1, {
            println("web server started")
            val bossGroup = NioEventLoopGroup(1)
            val workerGroup = NioEventLoopGroup()
            val b = ServerBootstrap()
            val initializer = Initializer(handlerThreadsCount)
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel().javaClass)
                    .childHandler(initializer)

            try {
                val channel = b.bind(webServerPort).sync().channel()
                channel.closeFuture().sync()
            } catch (e: InterruptedException) {
                println("web server stopped")
            } finally {
                bossGroup.shutdownGracefully()
                workerGroup.shutdownGracefully()
                initializer.group.shutdownGracefully()
            }
        })
    }
}