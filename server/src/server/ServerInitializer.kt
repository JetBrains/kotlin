package server

import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelPipeline
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpResponseEncoder
import io.netty.util.concurrent.DefaultEventExecutorGroup
import io.netty.util.concurrent.EventExecutorGroup

/**
 * Created by user on 7/6/16.
 */
class ServerInitializer : ChannelInitializer<SocketChannel> {

    val group: EventExecutorGroup;

    constructor(handlerThreadCount: Int) {
        this.group = DefaultEventExecutorGroup(handlerThreadCount)
    }

    override fun initChannel(channel: SocketChannel) {
        val p: ChannelPipeline = channel.pipeline()
        p.addLast(HttpRequestDecoder())
        p.addLast(HttpResponseEncoder())
        p.addLast(group, ServerHandler())
    }
}