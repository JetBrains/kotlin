package net.car.server

import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelPipeline
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.util.concurrent.DefaultEventExecutorGroup
import io.netty.util.concurrent.EventExecutorGroup
import net.Routes
import net.ServerHandler
import net.car.server.handlers.CarConnection

class Initializer : ChannelInitializer<SocketChannel> {

    val group: EventExecutorGroup

    constructor(handlerThreadCount: Int) {
        this.group = DefaultEventExecutorGroup(handlerThreadCount)
    }

    override fun initChannel(channel: SocketChannel) {
        val p: ChannelPipeline = channel.pipeline()

        p.addLast(HttpServerCodec())
        p.addLast(group, ServerHandler(mapOf(Pair(Routes.CAR_CONNECT, CarConnection()))))
    }
}