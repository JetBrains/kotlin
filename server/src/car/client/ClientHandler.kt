package car.client

import com.google.protobuf.InvalidProtocolBufferException
import getLocationUrl
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.HttpContent
import io.netty.util.AttributeKey
import objects.Environment
import proto.car.LocationP
import setRouteUrl
import java.util.*

/**
 * Created by user on 7/8/16.
 */
class ClientHandler : SimpleChannelInboundHandler<Any> {

    constructor()

    var contentBytes: ByteArray = ByteArray(0);

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        val url = ctx.channel().attr(AttributeKey.valueOf<String>("url")).get()
        val carUid = ctx.channel().attr(AttributeKey.valueOf<String>("uid")).get()
        val environment = Environment.instance
        when (url) {
            setRouteUrl -> {

            }
            getLocationUrl -> {

                try {
                    val data = LocationP.Location.parseFrom(contentBytes)
                    synchronized(environment, {
                        val car = environment.map.get(carUid)
                        if (car != null) {
                            car.x = data.x
                            car.y = data.y
                        }
                    })
                } catch (e: InvalidProtocolBufferException) {
                    //todo error answer
                }
            }
            else -> {

            }
        }
        ctx.close()
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: Any?) {
        if (msg is HttpContent) {
            val contentsBytes = msg.content();
            contentBytes = ByteArray(contentsBytes.capacity())
            contentsBytes.readBytes(contentBytes)
        }
    }
}