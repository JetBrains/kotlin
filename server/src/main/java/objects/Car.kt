package objects

import CodedOutputStream
import RouteMetricRequest
import SonarRequest
import net.car.client.Client
import org.asynchttpclient.ListenableFuture
import org.asynchttpclient.Response
import roomScanner.CarController.Direction
import roomScanner.serialize

class Car constructor(val uid: Int, host: String, port: Int) {

    private val CHARGE_CORRECTION = 1.0//on full charge ok is 0.83 - 0.86

    var x = 0.0
    var y = 0.0
    var angle = 0.0
    val carConnection = CarConnection(host, port)

    fun moveCar(distance: Int, direction: Direction): ListenableFuture<Response> {

        val route = RouteMetricRequest.BuilderRouteMetricRequest(
                IntArray(1, { (distance * CHARGE_CORRECTION).toInt() }), IntArray(1, { direction.id }))
        val bytesRoute = ByteArray(route.getSizeNoTag())
        route.writeTo(CodedOutputStream(bytesRoute))
        return carConnection.sendRequest(Client.Request.ROUTE_METRIC, bytesRoute)
    }

    fun scan(angles: IntArray, attempts: Int, windowSize: Int, smoothing: SonarRequest.Smoothing): ListenableFuture<Response> {
        val message = SonarRequest.BuilderSonarRequest(
                angles = angles,
                attempts = IntArray(angles.size, { attempts }),
                smoothing = smoothing,
                windowSize = windowSize)
                .build()
        val data = serialize(message.getSizeNoTag(), { message.writeTo(it) })
        return carConnection.sendRequest(Client.Request.SONAR, data)
    }

    override fun toString(): String {
        return "$uid ; x:$x; y:$y; target:$angle"
    }
}