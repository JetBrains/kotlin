package algorithm

import CodedOutputStream
import Exceptions.InactiveCarException
import RouteMetricRequest
import SonarRequest
import algorithm.geometry.Angle
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
import kotlin.concurrent.thread

abstract class AbstractAlgorithm(val thisCar: Car, val exchanger: Exchanger<IntArray>) {

    open val ATTEMPTS = 1
    open val SMOOTHING = SonarRequest.Smoothing.NONE
    open val WINDOW_SIZE = 0

    protected val FORWARD = 0
    protected val BACKWARD = 1
    protected val LEFT = 2
    protected val RIGHT = 3

    private var prevState: CarState? = null

    private var prevSonarDistances = mapOf<Angle, AngleData>()
    private val defaultAngles = arrayOf(Angle(0), Angle(70), Angle(75), Angle(80), Angle(85), Angle(90), Angle(95), Angle(100), Angle(105), Angle(110), Angle(180))
    //    private val defaultAngles = arrayOf(Angle(0), Angle(60), Angle(70), Angle(80), Angle(90), Angle(100), Angle(110), Angle(120), Angle(180))
    protected var requiredAngles = defaultAngles

    protected enum class CarState {
        WALL,
        INNER,
        OUTER
    }

    private var iterationCounter = 0

    protected fun getData(angles: Array<Angle>): IntArray {

        val anglesIntArray = (angles.map { it -> it.degs() }).toIntArray()
        val message = SonarRequest.BuilderSonarRequest(
                angles = anglesIntArray,
                attempts = IntArray(angles.size, { ATTEMPTS }),
                smoothing = SMOOTHING,
                windowSize = WINDOW_SIZE)
                .build()
        val requestBytes = ByteArray(message.getSizeNoTag())
        message.writeTo(CodedOutputStream(requestBytes))
        val request = getDefaultHttpRequest(thisCar.host, sonarUrl, requestBytes)
        try {
            car.client.Client.sendRequest(request, thisCar.host, thisCar.port, mapOf(Pair("angles", anglesIntArray)))
        } catch (e: InactiveCarException) {
            println("connection error!")
        }

        try {
            val distances = exchanger.exchange(IntArray(0), 300, TimeUnit.SECONDS)
            return distances
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
        } catch (e: TimeoutException) {
            println("don't have response from car. Timeout!")
        }
        return
    }

    fun iterate() {
        Logger.log("============= STARTING ITERATION ${iterationCounter} ============")
        iterationCounter++
        if (RoomModel.finished) {
            return
        }
        val angles = getAngles()
        val distances = getData(angles)
        if (distances.size != angles.size) {
            throw RuntimeException("error! angles and distances have various sizes")
        }
        val anglesDistances = mutableMapOf<Angle, AngleData>()
        for (i in 0..angles.size - 1) {
            anglesDistances.put(angles[i], AngleData(angles[i], distances[i]))
        }

        this.requiredAngles = defaultAngles

        val state = getCarState(anglesDistances) ?: return
        val command = getCommand(anglesDistances, state)

        if (command == null) {
            return
        }

        afterGetCommand(command)
        Logger.log("Sending command:")
        Logger.log("    Directions = ${Arrays.toString(command.directions)}")
        Logger.log("    Distanced = ${Arrays.toString(command.distances)}")
        println(Arrays.toString(command.directions))
        println(Arrays.toString(command.distances))

        this.prevSonarDistances = anglesDistances
        this.prevState = state

        moveCar(command)
        Logger.log("============= FINISHING ITERATION ${iterationCounter} ============")
        Logger.log("")
    }


    protected fun getPrevState(): CarState? {
        return prevState
    }

    protected fun getPrevSonarDistances(): Map<Angle, AngleData> {
        return prevSonarDistances
    }

    private fun getAngles(): Array<Angle> {
        return requiredAngles
    }

    protected abstract fun getCarState(anglesDistances: Map<Angle, AngleData>): CarState?
    protected abstract fun getCommand(anglesDistances: Map<Angle, AngleData>, state: CarState): RouteMetricRequest?
    protected abstract fun afterGetCommand(route: RouteMetricRequest)


    private fun getDefaultHttpRequest(host: String, url: String, bytes: ByteArray): DefaultFullHttpRequest {
        val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, url, Unpooled.copiedBuffer(bytes))
        request.headers().set(HttpHeaderNames.HOST, host)
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
        request.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes())
        return request
    }

}