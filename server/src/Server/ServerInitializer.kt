package Server

import Server.ServerHandler
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelPipeline
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpResponseEncoder

/**
 * Created by user on 7/6/16.
 */
class ServerInitializer : ChannelInitializer<SocketChannel> {

    constructor() {

    }

    override fun initChannel(channel: SocketChannel) {
        println("init")
        val p: ChannelPipeline = channel.pipeline()
        p.addLast(HttpRequestDecoder())
        p.addLast(HttpResponseEncoder())
        p.addLast(ServerHandler())
    }
}