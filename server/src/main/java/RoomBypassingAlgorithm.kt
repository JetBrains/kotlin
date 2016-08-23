import Exceptions.InactiveCarException
import car.client.Client
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import objects.Car
import java.util.concurrent.Exchanger
import java.util.concurrent.TimeUnit

object RoomBypassingAlgorithm {

    private val FORWARD = 0
    private val BACKWARD = 1
    private val LEFT = 2
    private val RIGHT = 3

    val exchanger: Exchanger<IntArray> = Exchanger()
    var thisCar: Car? = null

    private fun getData(angles: IntArray): DoubleArray {

        val copyElemCount = 5
        val car = thisCar
        if (car == null) {
            return DoubleArray(0)
        }
        val anglesCoped = IntArray(angles.size * copyElemCount, { angles[it % copyElemCount] })

        val message = SonarRequest.BuilderSonarRequest(anglesCoped).build()
        val requestBytes = ByteArray(message.getSizeNoTag())
        message.writeTo(CodedOutputStream(requestBytes))
        val request = getDefaultHttpRequest(car.host, sonarUrl, requestBytes)
        try {
            Client.sendRequest(request, car.host, car.port, mapOf(Pair("angles", anglesCoped)))
        } catch (e: InactiveCarException) {
            println("connection error!")
        }

        try {
            val distances = exchanger.exchange(IntArray(0), 20, TimeUnit.SECONDS)
            val result = DoubleArray(angles.size)
            for (i in 0..result.size - 1) {
                result[i] = distances.drop(i * copyElemCount).take(copyElemCount).sum().toDouble() / copyElemCount
            }
            return result
        } catch (e: InterruptedException) {
            println("don't have response from car!")
        }
        return DoubleArray(0)
    }

    private fun moveCar(direction: Int, time: Int) {
        val car = thisCar
        if (car == null) {
            return
        }
        val message = RouteRequest.BuilderRouteRequest(IntArray(1, { time }), IntArray(1, { direction }))
        val requestBytes = ByteArray(message.getSizeNoTag())
        message.writeTo(CodedOutputStream(requestBytes))
        val request = getDefaultHttpRequest(car.host, sonarUrl, requestBytes)
        try {
            Client.sendRequest(request, car.host, car.port, mapOf<String, Int>())
        } catch (e: InactiveCarException) {
            println("connection error!")
        }
        try {
            val distances = exchanger.exchange(IntArray(0), 20, TimeUnit.SECONDS)
            return
        } catch (e: InterruptedException) {
            println("don't have response from car!")
        }
        return
    }

    fun iterate(): Boolean {
        val angles = listOf(0, 60, 90, 120).toIntArray()
        val distances = getData(angles)
        if (distances.size != angles.size) {
            println("error! angles and distances have various sizes")
            return false
        }
        val anglesDistances = mutableMapOf<Int, Double>()
        for (i in 0..angles.size - 1) {
            anglesDistances.put(angles[i], distances[i])
        }
        val command = getCommand(anglesDistances)


        return true
    }

    private fun getIntArray(vararg args: Int): IntArray {
        return args
    }

    private fun getCommand(anglesDistances: MutableMap<Int, Double>): RouteRequest {
        val dist0 = anglesDistances[0]!!
        val dist60 = anglesDistances[60]!!
        val dist90 = anglesDistances[90]!!
        val dist120 = anglesDistances[120]!!
        val resultBuilder = RouteRequest.BuilderRouteRequest(IntArray(0), IntArray(0))
        if (Math.abs(dist120 - dist60) > 10) {
            val rotationDirection = getRotationDirection(dist60, dist120)
            resultBuilder.setDirections(getIntArray(rotationDirection))
            resultBuilder.setTimes(getIntArray(1000))
            return resultBuilder.build()
        }

        if (dist90 > 40) {
            resultBuilder.setDirections(getIntArray(RIGHT))
            resultBuilder.setTimes(getIntArray(1000))
            return resultBuilder.build()
        }
        if (dist90 < 20) {
            resultBuilder.setDirections(getIntArray(LEFT))
            resultBuilder.setTimes(getIntArray(1000))
            return resultBuilder.build()
        }

        //TODO учет внешнего угла. В нем может резко (да и не резко) вырасти расстояние на 60 градусов


        //крутиться не надо, стоим более-менее паралельно стене справа. Смотрим вперед, далеко ли угол (внутренний)
        if (dist0 > 100) {
            resultBuilder.setDirections(getIntArray(FORWARD))
            resultBuilder.setTimes(getIntArray(1000))
            return resultBuilder.build()
        } else {
            resultBuilder.setDirections(getIntArray(LEFT, FORWARD))
            resultBuilder.setTimes(getIntArray(500, 1000))
            return resultBuilder.build()
        }
    }

    private fun getRotationDirection(dist60: Double, dist120: Double): Int {
        if (dist120 > dist60) {
            return LEFT
        } else {
            return RIGHT
        }
    }

    private fun calculateAngleWithWall(anglesDistances: MutableMap<Int, Double>): Double {
        val dist60 = anglesDistances[60]!!
        val dist120 = anglesDistances[120]!!

        //Math.cos(60) = 1/2
        val wallLength = Math.sqrt(Math.pow(dist60, 2.0) + Math.pow(dist120, 2.0) - dist120 * dist60)//in triangle

        val hOnWall = getRangeToWall(wallLength, dist60, dist120)
        return 0.0
    }

    //return height in triangle on side a
    private fun getRangeToWall(a: Double, b: Double, c: Double): Double {
        val halfPerimeter = (a + b + c) / 2
        return Math.sqrt(halfPerimeter * (halfPerimeter - a) * (halfPerimeter - b) * (halfPerimeter - c)) * 2 / a
    }


    private fun getDefaultHttpRequest(host: String, url: String, bytes: ByteArray): DefaultFullHttpRequest {
        val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, url, Unpooled.copiedBuffer(bytes))
        request.headers().set(HttpHeaderNames.HOST, host)
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
        request.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes())
        return request
    }

}