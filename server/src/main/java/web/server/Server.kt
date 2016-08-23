package web.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import kotlin.concurrent.thread

object Server {
    private val handlerThreadsCount: Int = 10
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