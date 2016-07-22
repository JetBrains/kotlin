package car.server

import com.google.protobuf.InvalidProtocolBufferException
import connectUrl
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import objects.Environment
import routeDoneUrl
import ConnectionRequest
import ConnectionResponse
import java.io.ByteArrayInputStream
import CodedInputStream
import CodedOutputStream
import java.io.ByteArrayOutputStream
import RouteDoneRequest

/**
 * Created by user on 7/6/16.
 */
class Handler : SimpleChannelInboundHandler<Any>() {

    var url: String = ""
    var contentBytes: ByteArray = ByteArray(0);

    override fun channelReadComplete(ctx: ChannelHandlerContext) {

        val environment = Environment.instance
        var success = true;
        val answer: ByteArrayOutputStream = ByteArrayOutputStream()
        when (url) {
            connectUrl -> {
                val data = ConnectionRequest.BuilderConnectionRequest().build();
                try {
                    data.mergeFrom(CodedInputStream(ByteArrayInputStream(contentBytes)))
                } catch (e: InvalidProtocolBufferException) {
                    success = false;
                }
                if (success) {
                    val uid = environment.connectCar(data.ip, data.port)

                    ConnectionResponse.BuilderConnectionResponse().setUid(uid).build().writeTo(CodedOutputStream(answer))
                }
                //todo return connection error
            }
            routeDoneUrl -> {
                val data = RouteDoneRequest.BuilderRouteDoneRequest().build()
                try {
                    data.mergeFrom(CodedInputStream(ByteArrayInputStream(contentBytes)))
                } catch (e: InvalidProtocolBufferException) {
                    success = false;
                }
                if (success) {
                    val id = data.uid
                    synchronized(environment.map, {
                        val car = environment.map.get(id)
                        if (car != null) {
                            car.free = true
                        } else {
                            success = false
                        }
                    })
                    //todo return connection error
                }
            }
            else -> {
                success = false
            }
        }
        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, if (success) HttpResponseStatus.OK else HttpResponseStatus.BAD_REQUEST, Unpooled.copiedBuffer(answer.toByteArray()))
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes())
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is HttpRequest) {
            this.url = msg.uri()
        }

        if (msg is DefaultHttpContent) {
            val contentsBytes = msg.content();
            contentBytes = ByteArray(contentsBytes.capacity())
            contentsBytes.readBytes(contentBytes)
        }
    }


    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        println("exception")
        cause?.printStackTrace()
        if (ctx != null) {
            ctx.close()
        }
    }
}