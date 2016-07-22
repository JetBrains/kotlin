package web.server

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler

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