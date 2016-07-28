package car.client

import Exceptions.InactiveCarException
import io.netty.bootstrap.Bootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.HttpRequest
import io.netty.util.AttributeKey
import java.net.ConnectException

/**
 * Created by user on 7/8/16.
 */
object Client {

    val bootstrap: Bootstrap = makeBootstrap()

    private fun makeBootstrap(): Bootstrap {
        val group = NioEventLoopGroup()
        val b = Bootstrap()
        b.group(group).channel(NioSocketChannel().javaClass).handler(ClientInitializer())
                .attr(AttributeKey.newInstance<String>("url"), "")
                .attr(AttributeKey.newInstance<Int>("uid"), 0)
        return b
    }

    fun sendRequest(request: HttpRequest, host: String, port: Int, carUid: Int) {
        try {
            bootstrap.attr(AttributeKey.valueOf<String>("url"), request.uri())
            bootstrap.attr(AttributeKey.valueOf<Int>("uid"), carUid)
            val ch = bootstrap.connect(host, port).sync().channel()
            ch.writeAndFlush(request)
            ch.closeFuture().sync()//wait for answer
        } catch (e: InterruptedException) {

        } catch (e: ConnectException) {
            throw InactiveCarException()
        }
    }

}