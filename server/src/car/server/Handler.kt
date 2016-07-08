package car.server

import com.google.protobuf.InvalidProtocolBufferException
import connectUrl
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import objects.Car
import objects.Environment
import proto.car.ConnectP
import proto.car.ConnectP.ConnectionRequest
import proto.car.RouteDoneP.RouteDone
import routeDoneUrl
import java.util.*

/**
 * Created by user on 7/6/16.
 */
class Handler : SimpleChannelInboundHandler<Any>() {

    var url: String = ""
    var contentBytes: ByteArray = ByteArray(0);

    override fun channelReadComplete(ctx: ChannelHandlerContext) {

        val environment = Environment.instance
        var success = true;

        var answer:ByteArray = ByteArray(0)
        when (url) {
            connectUrl -> {
                var data: ConnectionRequest? = null
                try {
                    data = ConnectionRequest.parseFrom(contentBytes)
                } catch (e: InvalidProtocolBufferException) {
                    //todo error answer
                }
                if (data != null) {
                    val uid = environment.connectCar(data.ip, data.port)
                    answer = ConnectP.ConnectionResponse.newBuilder().setUid(uid).build().toByteArray()
                }
            }
            routeDoneUrl -> {
                var data: RouteDone? = null
                try {
                    data = RouteDone.parseFrom(contentBytes)
                } catch (e: InvalidProtocolBufferException) {
                    //todo error answer
                }
                if (data != null) {
                    val id = data.uid
                    synchronized(environment.map, {
                        val car = environment.map.get(id)
                        if (car != null) {
                            car.free = true
                        } else {
                            success = false
                        }
                    })
                }
            }
            else -> {
                success = false
                //todo unsup operation. error answer
            }
        }

        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, if (success) HttpResponseStatus.OK else HttpResponseStatus.NOT_FOUND, Unpooled.copiedBuffer(answer))
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