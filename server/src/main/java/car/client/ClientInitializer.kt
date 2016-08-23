package car.client

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpClientCodec

class ClientInitializer : ChannelInitializer<SocketChannel> {

    constructor()

    override fun initChannel(ch: SocketChannel) {
        val p = ch.pipeline()
        p.addLast(HttpClientCodec())
        p.addLast(ClientHandler())
    }
}