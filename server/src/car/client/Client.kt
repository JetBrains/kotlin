package car.client

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.HttpRequest
import io.netty.util.AttributeKey

/**
 * Created by user on 7/8/16.
 */
object Client {

    val bootstrap: Bootstrap = makeBootstrap()

    private fun makeBootstrap(): Bootstrap {
        val group = NioEventLoopGroup();
        val b = Bootstrap();
        b.group(group).channel(NioSocketChannel().javaClass).handler(ClientInitializer())
                .attr(AttributeKey.newInstance<String>("url"), "")
                .attr(AttributeKey.newInstance<String>("uid"), "")
        return b
    }

    fun sendRequest(request: HttpRequest, host: String, port: Int, carUid: String) {
        try {
            bootstrap.attr(AttributeKey.valueOf<String>("url"), request.uri())
            bootstrap.attr(AttributeKey.valueOf<String>("uid"), carUid)
            val ch = bootstrap.connect(host, port).sync().channel()
            ch.writeAndFlush(request)
            ch.closeFuture().sync()//wait for answer
        } catch (e: InterruptedException) {

        }
    }

}