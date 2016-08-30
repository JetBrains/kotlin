package algorithm

import CodedOutputStream
import Exceptions.InactiveCarException
import RouteMetricRequest
import SonarRequest
import algorithm.geometry.AngleData
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import objects.Car
import setRouteMetricUrl
import sonarUrl
import java.util.*
import java.util.concurrent.Exchanger
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

abstract class AbstractAlgorithm(val thisCar: Car, val exchanger: Exchanger<IntArray>) {

    protected val FORWARD = 0
    protected val BACKWARD = 1
    protected val LEFT = 2
    protected val RIGHT = 3

    private var prevState: CarState? = null

    private var prevSonarDistances = mapOf<Int, AngleData>()
    private val defaultAngles = listOf(0, 60, 90, 120, 180).toIntArray()
    protected var requiredAngles = defaultAngles

    protected enum class CarState {
        WALL,
        INNER,
        OUTER
    }

    protected fun getData(angles: IntArray): IntArray {

        val attempts = 1
        val threshold = 0
        val smoothing = SonarRequest.Smoothing.NONE

        val message = SonarRequest.BuilderSonarRequest(
                angles = angles,
                attempts = IntArray(angles.size, { attempts }),
                windowSize = 0,
                smoothing = smoothing)
                .build()
        val requestBytes = ByteArray(message.getSizeNoTag())
        message.writeTo(CodedOutputStream(requestBytes))
        val request = getDefaultHttpRequest(thisCar.host, sonarUrl, requestBytes)
        try {
            car.client.Client.sendRequest(request, thisCar.host, thisCar.port, mapOf(Pair("angles", angles)))
        } catch (e: InactiveCarException) {
            println("connection error!")
        }

        try {
            val distances = exchanger.exchange(IntArray(0), 300, TimeUnit.SECONDS)
            return distances
        } catch (e: InterruptedException) {
            println("don't have response from car!")
        } catch (e: TimeoutException) {
            println("don't have response from car. Timeout!")
        }
        return IntArray(0)
    }

    protected fun moveCar(message: RouteMetricRequest) {
        val requestBytes = ByteArray(message.getSizeNoTag())
        message.writeTo(CodedOutputStream(requestBytes))
        moveCar(requestBytes)
    }

    private fun moveCar(messageBytes: ByteArray) {
        val request = getDefaultHttpRequest(thisCar.host, setRouteMetricUrl, messageBytes)
        try {
            car.client.Client.sendRequest(request, thisCar.host, thisCar.port, mapOf<String, Int>())
        } catch (e: InactiveCarException) {
            println("connection error!")
        }
        try {
            exchanger.exchange(IntArray(0), 60, TimeUnit.SECONDS)
            return
        } catch (e: InterruptedException) {
            println("don't have response from car!")
        } catch (e: TimeoutException) {
            println("don't have response from car. Timeout!")
        }
        return
    }

    fun iterate() {
        val angles = getAngles()
        val distances = getData(angles)
        if (distances.size != angles.size) {
            throw RuntimeException("error! angles and distances have various sizes")
        }
        val anglesDistances = mutableMapOf<Int, AngleData>()
        for (i in 0..angles.size - 1) {
            if (Math.abs(distances[i]) < 0.01) {
                continue
            }
            anglesDistances.put(angles[i], AngleData(angles[i], distances[i]))
        }

        this.requiredAngles = defaultAngles

        val state = getCarState(anglesDistances) ?: return
        val command = getCommand(anglesDistances, state)
        afterGetCommand(command)
        println(Arrays.toString(command.directions))
        println(Arrays.toString(command.distances))

        this.prevSonarDistances = anglesDistances
        this.prevState = state

        moveCar(command)
    }


    private fun getAngles(): IntArray {
        return requiredAngles
    }

    protected abstract fun getCarState(anglesDistances: Map<Int, AngleData>): CarState?
    protected abstract fun getCommand(anglesDistances: Map<Int, AngleData>, state: CarState): RouteMetricRequest
    protected abstract fun afterGetCommand(route: RouteMetricRequest)


    private fun getDefaultHttpRequest(host: String, url: String, bytes: ByteArray): DefaultFullHttpRequest {
        val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, url, Unpooled.copiedBuffer(bytes))
        request.headers().set(HttpHeaderNames.HOST, host)
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
        request.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes())
        return request
    }

}