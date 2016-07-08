package web.server

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
class Handler : SimpleChannelInboundHandler<Any>() {

    var content: StringBuilder = StringBuilder()

    override fun channelReadComplete(ctx: ChannelHandlerContext) {

        content.setLength(0)
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Any) {
        println(msg)
    }


    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        println("exception")
        cause?.printStackTrace()
        if (ctx != null) {
            ctx.close()
        }
    }
}