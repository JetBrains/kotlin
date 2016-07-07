package server

import connectUrl
import getLocationUrl
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil
import objects.Environment
import proto.car.ConnectP.ConnectionRequest
import proto.car.RouteDoneP
import proto.car.RouteDoneP.RouteDone
import routeDoneUrl
import setRouteUrl
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by user on 7/6/16.
 */
class ServerHandler : SimpleChannelInboundHandler<Any>() {

    var url: String = ""
    var contentBytes: ByteArray = ByteArray(0);

    override fun channelReadComplete(ctx: ChannelHandlerContext) {

        val environment = Environment.instance
        var success = true;
        when (url) {
            connectUrl -> {
                val data = ConnectionRequest.parseFrom(contentBytes)
                //todo
            }
            routeDoneUrl -> {
                val data = RouteDone.parseFrom(contentBytes)
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
            else -> {
                success = false
                //todo unsup operation
            }
        }

        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, if (success) HttpResponseStatus.OK else HttpResponseStatus.NOT_FOUND)
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes())
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is HttpRequest) {
            this.url = msg.uri()
//            sb.append("" + msg.method() + " " + msg.protocolVersion() + "\n" + msg.uri())
//            for (header in msg.headers()) {
//                sb.append(header.key + ":" + header.value + "\n")
//            }
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