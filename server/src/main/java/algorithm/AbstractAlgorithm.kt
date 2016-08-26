package algorithm

import CodedOutputStream
import Exceptions.InactiveCarException
import RouteRequest
import SonarRequest
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import objects.Car
import setRouteUrl
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

    private var prevSonarDistances = mapOf<Int, Double>()
    private val defaultAngles = listOf(0, 60, 90, 120, 180).toIntArray()
    protected var requiredAngles = defaultAngles

    protected enum class CarState {
        WALL,
        INNER,
        OUTER
    }

    protected fun getData(angles: IntArray): DoubleArray {

        val copyElemCount = 1
        val anglesCoped = IntArray(angles.size * copyElemCount, { angles[it / copyElemCount] })

        val message = SonarRequest.BuilderSonarRequest(anglesCoped).build()
        val requestBytes = ByteArray(message.getSizeNoTag())
        message.writeTo(CodedOutputStream(requestBytes))
        val request = getDefaultHttpRequest(thisCar.host, sonarUrl, requestBytes)
        try {
            car.client.Client.sendRequest(request, thisCar.host, thisCar.port, mapOf(Pair("angles", anglesCoped)))
        } catch (e: InactiveCarException) {
            println("connection error!")
        }

        try {
            val distances = exchanger.exchange(IntArray(0), 300, TimeUnit.SECONDS)
            val result = DoubleArray(angles.size)
            for (i in 0..result.size - 1) {
                val distancesOnCurrentAngle = distances.drop(i * copyElemCount).take(copyElemCount)
                var sum = 0
                for (distance in distancesOnCurrentAngle) {
                    if (distance != -1) {
                        sum += distance
                    }
                }
                if (sum != 0) {
                    result[i] = (sum.toDouble()) / (copyElemCount)
                }
            }
            return result
        } catch (e: InterruptedException) {
            println("don't have response from car!")
        } catch (e: TimeoutException) {
            println("don't have response from car. Timeout!")
        }
        return DoubleArray(0)
    }

    protected fun moveCar(message: RouteRequest) {
        val requestBytes = ByteArray(message.getSizeNoTag())
        message.writeTo(CodedOutputStream(requestBytes))
        val request = getDefaultHttpRequest(thisCar.host, setRouteUrl, requestBytes)
        try {
            car.client.Client.sendRequest(request, thisCar.host, thisCar.port, mapOf<String, Int>())
        } catch (e: InactiveCarException) {
            println("connection error!")
        }
        try {
            exchanger.exchange(IntArray(0), 20, TimeUnit.SECONDS)
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
        val anglesDistances = mutableMapOf<Int, Double>()
        for (i in 0..angles.size - 1) {
            if (Math.abs(distances[i]) < 0.01) {
                continue
            }
            anglesDistances.put(angles[i], distances[i])
        }

        this.requiredAngles = defaultAngles

        val state = getCarState(anglesDistances)
        if (state == null) {
            return
        }
        val command = getCommand(anglesDistances, state)
        afterGetCommand(command)
        println(Arrays.toString(command.directions))
        println(Arrays.toString(command.times))

        this.prevSonarDistances = anglesDistances
        this.prevState = state

        moveCar(command)
    }


    protected fun getPrevState(): CarState? {
        return prevState
    }

    protected fun getPrevSonarDistances(): Map<Int, Double> {
        return prevSonarDistances
    }

    private fun getAngles(): IntArray {
        return requiredAngles
    }

    protected abstract fun getCarState(anglesDistances: Map<Int, Double>): CarState?
    protected abstract fun getCommand(anglesDistances: Map<Int, Double>, state: CarState): RouteRequest
    protected abstract fun afterGetCommand(route:RouteRequest)


    private fun getDefaultHttpRequest(host: String, url: String, bytes: ByteArray): DefaultFullHttpRequest {
        val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, url, Unpooled.copiedBuffer(bytes))
        request.headers().set(HttpHeaderNames.HOST, host)
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
        request.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes())
        return request
    }

}