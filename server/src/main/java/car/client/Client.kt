package car.client

import Exceptions.InactiveCarException
import io.netty.bootstrap.Bootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.HttpRequest
import io.netty.util.AttributeKey
import org.asynchttpclient.DefaultAsyncHttpClient
import org.asynchttpclient.ListenableFuture
import org.asynchttpclient.Response
import java.net.ConnectException
import java.rmi.UnexpectedException

object Client {
    private val group = NioEventLoopGroup()
    private val bootstrap = makeBootstrap()
    private val client = DefaultAsyncHttpClient()
    private val timeout = 5 * 60 * 60 * 1000

    fun shutDownClient() {
        client.close()
        group.shutdownGracefully()
    }

    fun <T> sendRequest(request: HttpRequest, host: String, port: Int, options: Map<String, T>) {
        try {
            bootstrap.attr(AttributeKey.valueOf<String>("url"), request.uri())
            for ((key, value) in options) {
                bootstrap.attr(AttributeKey.valueOf<T>(key), value)
            }
            val ch = bootstrap.connect(host, port).sync().channel()
            ch.writeAndFlush(request)
//            ch.closeFuture().sync()//wait for answer
        } catch (e: InterruptedException) {

        } catch (e: ConnectException) {
            throw InactiveCarException()
        }
    }

    fun makeRequest(request: String, data: ByteArray): ListenableFuture<Response> =
            client.preparePost(request).setBody(data).setRequestTimeout(timeout).execute() ?: throw UnexpectedException(request)

    private fun makeBootstrap(): Bootstrap {
        val b = Bootstrap()
        b.group(group).channel(NioSocketChannel().javaClass).handler(ClientInitializer())
                .attr(AttributeKey.newInstance<String>("url"), "")
                .attr(AttributeKey.newInstance<Int>("uid"), 0)
                .attr(AttributeKey.newInstance<IntArray>("angles"), IntArray(0))
        return b
    }
}