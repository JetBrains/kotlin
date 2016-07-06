package Server

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by user on 7/6/16.
 */
class ServerHandler : SimpleChannelInboundHandler<Any>() {

    val sb: StringBuilder = StringBuilder();

    object static {
        val stf = SimpleDateFormat("HH-mm-ss");
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {

        val dateTime = ServerHandler.static.stf.format(Date(System.currentTimeMillis()))
        val fileName: String = "request_$dateTime.log";
        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes())
        ctx.writeAndFlush(response)
        writeToFile(sb.toString(), fileName);
    }


    override fun channelRead0(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is HttpRequest) {
            sb.append("" + msg.method() + " " + msg.protocolVersion() + "\n")
            for (header in msg.headers()) {
                sb.append(header.key + ":" + header.value + "\n")
            }
        }
        if (msg is DefaultHttpContent) {
            sb.append(msg.content().toString(CharsetUtil.UTF_8))
        }
    }

    fun writeToFile(toFile: String, fileName: String) {
        val f = File(fileName)
        if (!f.exists()) f.createNewFile()
        f.writeText(toFile);
    }
}