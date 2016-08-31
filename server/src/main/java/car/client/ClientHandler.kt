package car.client

import CodedInputStream
import DebugClInterface
import DebugResponseMemoryStats
import LocationResponse
import SonarResponse
import debugMemoryUrl
import getLocationUrl
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.DefaultHttpContent
import io.netty.util.AttributeKey
import objects.Environment
import setRouteMetricUrl
import setRouteUrl
import sonarUrl
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class ClientHandler : SimpleChannelInboundHandler<Any>() {

    var contentBytes: ByteArray = ByteArray(0)

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        val url = ctx.channel().attr(AttributeKey.valueOf<String>("url")).get()
        when (url) {
            getLocationUrl -> {
                val carUid = ctx.channel().attr(AttributeKey.valueOf<Int>("uid")).get()
                val locData = LocationResponse.LocationData.BuilderLocationData(0, 0, 0).build()
                val response = LocationResponse.BuilderLocationResponse(locData, 0).build()
                response.mergeFrom(CodedInputStream(contentBytes))
                handlerGetLocationResponse(response, carUid)
            }
            debugMemoryUrl -> {
                val response = DebugResponseMemoryStats.BuilderDebugResponseMemoryStats(0, 0, 0, 0).build()
                response.mergeFrom(CodedInputStream(contentBytes))
                handlerDebugMemoryResponse(response)
            }
            sonarUrl -> {
                val response = SonarResponse.BuilderSonarResponse(IntArray(0)).build()
                response.mergeFrom(CodedInputStream(contentBytes))
                val angles = ctx.channel().attr(AttributeKey.valueOf<IntArray>("angles")).get()
                handlerSonarResponse(response, angles)
            }
            setRouteUrl, setRouteMetricUrl -> {
                try {
                    DebugClInterface.exchanger.exchange(IntArray(0), 20, TimeUnit.SECONDS)
                } catch (e: InterruptedException) {
                    println("interrupted before sending datas to algorithm!")
                } catch (e: TimeoutException) {
                    e.printStackTrace()
                    println("timeout")
                }
            }
            else -> {

            }
        }
        ctx.close()
    }

    private fun handlerGetLocationResponse(message: LocationResponse, carUid: Int) {
        if (message.code == 0) {
            val data = message.locationResponseData
            synchronized(Environment, {
                val car = Environment.map[carUid]
                if (car != null) {
                    car.x = data.x
                    car.y = data.y
                    car.angle = data.angle
                }
            })
        }
    }

    private fun handlerDebugMemoryResponse(message: DebugResponseMemoryStats) {
        println("heapDynamicMaxBytes ${message.heapDynamicMaxBytes}")
        println("heapDynamicTail ${message.heapDynamicTail}")
        println("heapDynamicTotalBytes ${message.heapDynamicTotalBytes}")
        println("heapStaticTail ${message.heapStaticTail}")
    }

    private fun handlerSonarResponse(message: SonarResponse, angles: IntArray) {
        println("request angles: ${Arrays.toString(angles)}")
        println("distances from sonar: ${Arrays.toString(message.distances)}")
        val algThread = DebugClInterface.algorithmThread
        if (algThread == null) {
            return
        }
        if (!algThread.isAlive) {
            return
        }
        try {
            DebugClInterface.exchanger.exchange(message.distances, 20, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            println("interrupted before sending datas to algorithm!")
        } catch (e: TimeoutException) {
            e.printStackTrace()
            println("timeout")
        }
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: Any?) {
        if (msg is DefaultHttpContent) {
            // TODO: refactor variable names. It's hard as hell to read code with "contentBytes" and "content(!)s(!)Bytes" in lines of code.
            val contentsBytes = msg.content()
            contentBytes = ByteArray(contentsBytes.capacity())
            contentsBytes.readBytes(contentBytes)
        }
    }
}
