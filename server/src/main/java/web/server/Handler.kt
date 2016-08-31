package web.server

import CodedInputStream
import CodedOutputStream
import DirectionRequest
import GenericResponse
import ModeChange
import Result
import Waypoints
import DebugResponse
import algorithm.RoomModel
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil
import java.util.*

class Handler : SimpleChannelInboundHandler<Any>() {

    var contentBytes: ByteArray = ByteArray(0)
    var url: String? = null
    var method: HttpMethod? = null

    fun encodeProtoInHttpResponse(outs: CodedOutputStream): DefaultHttpResponse {
        val response = DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(Base64.getEncoder().encodeToString(outs.buffer), CharsetUtil.UTF_8)
        )
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes())
        response.headers().add("Access-Control-Allow-Origin", "*")
        response.headers().add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS")
        response.headers().add("Access-Control-Allow-Headers", "X-Requested-With, Content-Direction, Content-Length")
        return response
    }

    fun respondWith(ctx: ChannelHandlerContext, msg: GenericResponse) {
        val outs = CodedOutputStream(ByteArray(msg.getSizeNoTag()))
        msg.writeTo(outs)
        val response = encodeProtoInHttpResponse(outs)
        ctx.writeAndFlush(response).addListener(io.netty.channel.ChannelFutureListener.CLOSE)
    }

    fun respondWith(ctx: ChannelHandlerContext, msg: Waypoints) {
        val outs = CodedOutputStream(ByteArray(msg.getSizeNoTag()))
        msg.writeTo(outs)
        val response = encodeProtoInHttpResponse(outs)
        ctx.writeAndFlush(response).addListener(io.netty.channel.ChannelFutureListener.CLOSE)
    }

    fun respondWith(ctx: ChannelHandlerContext, msg: DebugResponse) {
        val outs = CodedOutputStream(ByteArray(msg.getSizeNoTag()))
        msg.writeTo(outs)
        val response = encodeProtoInHttpResponse(outs)
        ctx.writeAndFlush(response).addListener(io.netty.channel.ChannelFutureListener.CLOSE)
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        when (url) {
            Constants.changeModeURL -> {
                // Parse mode change request
                val ins = CodedInputStream(Base64.getDecoder().decode(contentBytes))
                val request = ModeChange.BuilderModeChange(ModeChange.Mode.fromIntToMode(0)).parseFrom(ins)
                val requestedMode = Server.ServerMode.fromProtoMode(request.newMode)

                if (Server.serverMode != Server.ServerMode.IDLE && requestedMode != Server.ServerMode.IDLE) {
                    println("Can't change server mode from ${Server.serverMode.toString()} to ${requestedMode.toString()}")
                    val protoResponse = GenericResponse.BuilderGenericResponse(Result.BuilderResult(1).build()).build()
                    respondWith(ctx, protoResponse)
                    return
                }

                // Change server mode
                Server.changeMode(requestedMode)

                // Respond with "OK"-protobuf
                val protoResponse = GenericResponse.BuilderGenericResponse(Result.BuilderResult(0).build()).build()
                respondWith(ctx, protoResponse)
            }
            Constants.getWaypointsURL -> {
                if (Server.serverMode != Server.ServerMode.PERIMETER_BUILDING) {
                    println("Can't get waypoints when in not Permiter Building mode")
                    val protoResponse = GenericResponse.BuilderGenericResponse(Result.BuilderResult(1).build()).build()
                    respondWith(ctx, protoResponse)
                    return
                }

                // Send update for UI
                val msg = RoomModel.getUpdate()
                respondWith(ctx, msg)
            }
            Constants.directionOrderURL -> {
                if (Server.serverMode != Server.ServerMode.MANUAL_MODE) {
                    println("Can't execute move order when not in manual mode")
                    val protoResponse = GenericResponse.BuilderGenericResponse(Result.BuilderResult(1).build()).build()
                    respondWith(ctx, protoResponse)
                    return
                }

                // Parse direction order
                val ins = CodedInputStream(contentBytes)
                val order = DirectionRequest.BuilderDirectionRequest(DirectionRequest.Command.FORWARD, 0, false).parseFrom(ins)
                if (order.stop) {
                    web.server.Server.changeMode(Server.ServerMode.IDLE)
                    val protoResponse = GenericResponse.BuilderGenericResponse(Result.BuilderResult(1).build()).build()
                    respondWith(ctx, protoResponse)
                    return
                }

                sendCarOrder(order.command)

                // TODO: should be done as callback after sending car order
                // Send update back
                val msg = RoomModel.getUpdate()
                respondWith(ctx, msg)
            }
            Constants.getDebug -> {
                val msg = RoomModel.getDebugInfo()
                respondWith(ctx, msg)
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