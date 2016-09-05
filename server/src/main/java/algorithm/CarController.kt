package algorithm

import CodedInputStream
import roomScanner.serialize
import RouteMetricRequest
import SonarRequest
import SonarResponse
import net.car.client.Client
import objects.CarConnection
import java.net.ConnectException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


class CarController(val carConnection: CarConnection) {

    fun scan(angles: IntArray, attempts: Int, windowSize: Int, smoothing: SonarRequest.Smoothing): IntArray {
        val message = SonarRequest.BuilderSonarRequest(
                angles = angles,
                attempts = IntArray(angles.size, { attempts }),
                smoothing = smoothing,
                windowSize = windowSize)
                .build()
        val data = serialize(message.getSizeNoTag(), { message.writeTo(it) })
        val response: ByteArray
        try {
            val future = carConnection.sendRequest(Client.Request.SONAR, data)
            response = future.get(300, TimeUnit.SECONDS).responseBodyAsBytes

        } catch (e: ConnectException) {
            println("connection error!")
            return IntArray(0)
        } catch (e: TimeoutException) {
            println("don't have response from net.car. Timeout!")
            return IntArray(0)
        }
        return SonarResponse.BuilderSonarResponse(IntArray(0)).parseFrom(CodedInputStream(response)).build().distances
    }

    fun moveCar(message: RouteMetricRequest) {
        val requestBytes = serialize(message.getSizeNoTag(), { message.writeTo(it) })
        moveCar(requestBytes)
    }

    fun moveCar(distances: IntArray, directions: IntArray) {
        val routeMetric = RouteMetricRequest.BuilderRouteMetricRequest(distances, directions).build()
        moveCar(routeMetric)
    }

    private fun moveCar(messageBytes: ByteArray) {
        try {
            carConnection.sendRequest(Client.Request.ROUTE_METRIC, messageBytes).get(60, TimeUnit.SECONDS)
        } catch (e: ConnectException) {
            println("connection error!")
        } catch (e: TimeoutException) {
            println("don't have response from net.car. Timeout!")
        }
        return
    }
}