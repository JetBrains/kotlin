package car.server

import CodedInputStream
import CodedOutputStream
import ConnectionRequest
import ConnectionResponse
import connectUrl
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import objects.Environment

class Handler : SimpleChannelInboundHandler<Any>() {

    var url: String = ""
    var contentBytes: ByteArray = ByteArray(0)

    override fun channelReadComplete(ctx: ChannelHandlerContext) {

        val environment = Environment.instance
        var success = true
        var answer = ByteArray(0)
        when (url) {
            connectUrl -> {
                val data = ConnectionRequest.BuilderConnectionRequest(IntArray(0), 0).build()
                data.mergeFrom(CodedInputStream(contentBytes))
                val ipStr = data.ipValues.map { elem -> elem.toString() }.reduce { elem1, elem2 -> elem1 + "." + elem2 }
                val uid = environment.connectCar(ipStr, data.port)
                val responseObject = ConnectionResponse.BuilderConnectionResponse(uid, 0).build()
                answer = ByteArray(responseObject.getSizeNoTag())
                responseObject.writeTo(CodedOutputStream(answer))
            }
            else -> {
                success = false
            }
        }
        val responseStatus = if (success) HttpResponseStatus.OK else HttpResponseStatus.BAD_REQUEST
        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, responseStatus, Unpooled.copiedBuffer(answer))
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes())
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is HttpRequest) {
            this.url = msg.uri()
        }

        if (msg is DefaultHttpContent) {
            val contentsBytes = msg.content()
            contentBytes = ByteArray(contentsBytes.capacity())
            contentsBytes.readBytes(contentBytes)
        }
    }


    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        println("exception")
        cause?.printStackTrace()
        ctx?.close()
    }
}