package server

import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelPipeline
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpResponseEncoder
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.util.concurrent.DefaultEventExecutorGroup
import io.netty.util.concurrent.EventExecutorGroup

/**
 * Created by user on 7/6/16.
 */
class Initializer : ChannelInitializer<SocketChannel> {


    constructor() {
    }

    override fun initChannel(channel: SocketChannel) {
        val p: ChannelPipeline = channel.pipeline()

        p.addLast(HttpServerCodec())
//        p.addLast(HttpRequestDecoder())
//        p.addLast(HttpResponseEncoder())
        p.addLast(Handler())
    }
}