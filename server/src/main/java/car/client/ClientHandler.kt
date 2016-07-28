package car.client

import CodedInputStream
import InvalidProtocolBufferException
import LocationResponse
import getLocationUrl
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.DefaultHttpContent
import io.netty.util.AttributeKey
import objects.Environment
import java.io.ByteArrayInputStream
import java.util.*

/**
 * Created by user on 7/8/16.
 */
class ClientHandler : SimpleChannelInboundHandler<Any> {

    constructor()

    var contentBytes: ByteArray = ByteArray(0);

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        val url = ctx.channel().attr(AttributeKey.valueOf<String>("url")).get()
        val carUid = ctx.channel().attr(AttributeKey.valueOf<Int>("uid")).get()
        val environment = Environment.instance
        when (url) {
            getLocationUrl -> {
                try {
                    val response = LocationResponse.BuilderLocationResponse().build()
                    response.mergeFrom(CodedInputStream(ByteArrayInputStream(contentBytes)))

                    if (response.code == 0) {
                        val data = response.locationResponseData
                        synchronized(environment, {
                            val car = environment.map.get(carUid)
                            if (car != null) {
                                car.x = data.x
                                car.y = data.y
                            }
                        })
                    }
                } catch (e: InvalidProtocolBufferException) {
                    println("invalic proto format!")
                }
            }
            else -> {

            }
        }
        ctx.close()
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: Any?) {
        if (msg is DefaultHttpContent) {
            val contentsBytes = msg.content();
            contentBytes = ByteArray(contentsBytes.capacity())
            contentsBytes.readBytes(contentBytes)
        }
    }
}