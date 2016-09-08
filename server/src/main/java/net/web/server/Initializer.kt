package net.web.server

import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelPipeline
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.util.concurrent.DefaultEventExecutorGroup
import io.netty.util.concurrent.EventExecutorGroup
import net.Routes
import net.ServerHandler
import net.web.server.handlers.ChangeMode
import net.web.server.handlers.DirectionOrder
import net.web.server.handlers.GetRoomModel

class Initializer : ChannelInitializer<SocketChannel> {

    val group: EventExecutorGroup

    constructor(handlerThreadCount: Int) {
        this.group = DefaultEventExecutorGroup(handlerThreadCount)
    }

    override fun initChannel(channel: SocketChannel) {
        val p: ChannelPipeline = channel.pipeline()
        p.addLast(HttpServerCodec())
        p.addLast(group, ServerHandler(mapOf(
                Pair(Routes.CHANGE_MODE_PATH, ChangeMode()),
                Pair(Routes.GET_DEBUG_PATH, GetRoomModel()),
                Pair(Routes.DIRECTION_ORDER_PATH, DirectionOrder()))))
    }
}