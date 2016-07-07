package server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler

/**
 * Created by user on 7/6/16.
 */
class Server : Runnable {

    val port: Int;
    val handlerThreadsCount: Int;

    constructor(port: Int, handlerThreadsCount: Int) {
        this.port = port
        this.handlerThreadsCount = handlerThreadsCount
    }

    override fun run() {
        val bossGroup = NioEventLoopGroup(1)
        val workerGroup = NioEventLoopGroup()
        val b = ServerBootstrap()
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel().javaClass)
                .handler(LoggingHandler(LogLevel.INFO))
                .childHandler(ServerInitializer(handlerThreadsCount))

        try {
            val channel = b.bind(port).sync().channel()
            channel.closeFuture().sync()
        } catch (e: InterruptedException) {
            e.printStackTrace()
            //todo
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }

    }

}