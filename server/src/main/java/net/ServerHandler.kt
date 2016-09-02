package net

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil
import java.util.*

class ServerHandler(val handlers: Map<String, Handler>) : SimpleChannelInboundHandler<Any>() {

    var contentBytes: ByteArray = ByteArray(0)
    var path: String = ""
    var method: HttpMethod? = null

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        val handler = handlers[path]
        if (handler == null) {
            ctx.close()
            return
        }
        val responseBytes = handler.execute(contentBytes)
        val response = encodeProtoInHttpResponse(responseBytes)
        ctx.writeAndFlush(response).addListener(io.netty.channel.ChannelFutureListener.CLOSE)
    }

    private fun encodeProtoInHttpResponse(responseBytes: ByteArray): DefaultHttpResponse {
        val response = DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(Base64.getEncoder().encodeToString(responseBytes), CharsetUtil.UTF_8)
        )
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes())
        response.headers().add("Access-Control-Allow-Origin", "*")
        response.headers().add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS")
        response.headers().add("Access-Control-Allow-Headers", "X-Requested-With, Content-Direction, Content-Length")
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes())
        return response
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is HttpRequest) {
            path = msg.uri()
            method = msg.method()
        }

        if (msg is DefaultHttpContent) {
            val contentBuffer = msg.content()
            contentBytes = ByteArray(contentBuffer.capacity())
            contentBuffer.readBytes(contentBytes)
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        println("exception")
        cause?.printStackTrace()
        ctx?.close()
    }
}