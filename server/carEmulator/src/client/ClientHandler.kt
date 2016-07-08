package client

import com.google.protobuf.InvalidProtocolBufferException
import connectUrl
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.DefaultHttpContent
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import io.netty.util.AttributeKey
import proto.car.ConnectP

/**
 * Created by user on 7/8/16.
 */
class ClientHandler : SimpleChannelInboundHandler<Any> {

    constructor()

    var url: String = ""
    var contentBytes: ByteArray = ByteArray(0);


    override fun channelReadComplete(ctx: ChannelHandlerContext) {

        val url = ctx.channel().attr(AttributeKey.valueOf<String>("url")).get()

        when (url) {
            connectUrl -> {
                try {
                    val uid = ConnectP.ConnectionResponse.parseFrom(contentBytes).uid
                    synchronized(ThisCar.instance, {
                        ThisCar.instance.id = uid;
                    })
                } catch (e: InvalidProtocolBufferException) {
                    e.printStackTrace()
                }
            }
            else -> {
                //todo error
            }
        }
        ctx.close()
    }

    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        super.channelRead(ctx, msg)
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: Any?) {
        if (msg is HttpResponse) {
            //            this.url = msg.uri()
        }

        if (msg is HttpContent) {
            val contentsBytes = msg.content();
            contentBytes = ByteArray(contentsBytes.capacity())
            contentsBytes.readBytes(contentBytes)
        }
    }
}