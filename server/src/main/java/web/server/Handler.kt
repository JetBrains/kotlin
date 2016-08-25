package web.server

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil
import GenericResponse
import CodedOutputStream
import CodedInputStream
import java.util.*

class Handler : SimpleChannelInboundHandler<Any>() {

    var contentBytes: ByteArray = ByteArray(0)
    var url: String? = null
    var method: HttpMethod? = null

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        when (url) {
            Constants.changeModeURL -> {
                if (Server.serverMode != Server.ServerMode.IDLE && Server.serverMode != Server.ServerMode.MANUAL_MODE) {
                    throw IllegalStateException("Can't change server mode in algorithm execution mode!")
                }

                // Parse mode change request
                val ins = CodedInputStream(Base64.getDecoder().decode(contentBytes))
                val request = ModeChange.BuilderModeChange(ModeChange.Mode.ManualControl).parseFrom(ins)

                // Change server mode
                Server.changeMode(Server.ServerMode.fromProtoMode(request.newMode))

                // Respond with "OK"-protobuf
                val protoResponse = GenericResponse.BuilderGenericResponse(Result.BuilderResult(0).build())
                val outs = CodedOutputStream(ByteArray(protoResponse.getSizeNoTag()))
                protoResponse.writeTo(outs)
                val response = DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK,
                        Unpooled.copiedBuffer(Base64.getEncoder().encodeToString(outs.buffer), CharsetUtil.UTF_8)
                    )
                response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes())
                response.headers().add("Access-Control-Allow-Origin", "*");
                response.headers().add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
                response.headers().add("Access-Control-Allow-Headers", "X-Requested-With, Content-Type, Content-Length");
                ctx.writeAndFlush(response).addListener(io.netty.channel.ChannelFutureListener.CLOSE)
            }
            Constants.getWaypointsURL -> {
                if (Server.serverMode != Server.ServerMode.PERIMETER_BUILDING) {
                    throw IllegalStateException("Can't get waypoints when in not Permiter Building mode")
                }

                // Build update for UI
                val msg = RoomModel.getUpdate()
                val outs = CodedOutputStream(ByteArray(msg.getSizeNoTag()))
                msg.writeTo(outs)
                val response = DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK,
                        Unpooled.copiedBuffer(Base64.getEncoder().encodeToString(outs.buffer), CharsetUtil.UTF_8)
                )
                response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes())
                response.headers().add("Access-Control-Allow-Origin", "*");
                response.headers().add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
                response.headers().add("Access-Control-Allow-Headers", "X-Requested-With, Content-Type, Content-Length");
                ctx.writeAndFlush(response).addListener(io.netty.channel.ChannelFutureListener.CLOSE)
            }
            Constants.directionOrderURL -> {
                if (Server.serverMode != Server.ServerMode.MANUAL_MODE) {
                    throw IllegalStateException("Can't execute move order when not in manual mode")
                }

                // Parse direction order
                val ins = CodedInputStream(contentBytes)
                val order = DirectionRequest.BuilderDirectionRequest(DirectionRequest.Command.FORWARD, 0, false).parseFrom(ins)
                if (order.stop) {
                    web.server.Server.changeMode(Server.ServerMode.IDLE)
                    return
                }

                sendCarOrder(order.command)

                // TODO: should be done as callback after sending car order
                // Send update back
                val msg = RoomModel.getUpdate()
                val outs = CodedOutputStream(ByteArray(msg.getSizeNoTag()))
                msg.writeTo(outs)
                val response = DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK,
                        Unpooled.copiedBuffer(Base64.getEncoder().encodeToString(outs.buffer), CharsetUtil.UTF_8)
                )
                response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes())
                response.headers().add("Access-Control-Allow-Origin", "*");
                response.headers().add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
                response.headers().add("Access-Control-Allow-Headers", "X-Requested-With, Content-Type, Content-Length");
                ctx.writeAndFlush(response).addListener(io.netty.channel.ChannelFutureListener.CLOSE)
            }
        }
    }

    // TODO: stub!!
    fun sendCarOrder(cmd: DirectionRequest.Command) {
        println("Sent order ${cmd.toString()}")
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is HttpRequest) {
            url = msg.uri()
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