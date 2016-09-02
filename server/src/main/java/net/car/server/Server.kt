package net.car.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import kotlin.concurrent.thread


object Server {

    private val DEFAULT_PORT = 7925
    private val handlerThreadsCount: Int = 100

    fun createCarServerThread(carServerPort: Int = DEFAULT_PORT): Thread {
        return thread(false, false, null, "carServer", -1, {
            println("net.car server started")
            val bossGroup = NioEventLoopGroup(1)
            val workerGroup = NioEventLoopGroup()
            val b = ServerBootstrap()
            val initializer = Initializer(handlerThreadsCount)
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel().javaClass)
                    .childHandler(initializer)

            try {
                val channel = b.bind(carServerPort).sync().channel()
                channel.closeFuture().sync()
            } catch (e: InterruptedException) {
                println("net.car server stopped")
            } finally {
                bossGroup.shutdownGracefully()
                workerGroup.shutdownGracefully()
                initializer.group.shutdownGracefully()
            }
        })
    }

}