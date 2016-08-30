package car.client

import objects.Car
import org.asynchttpclient.ListenableFuture
import org.asynchttpclient.Response

object CarClient {
    enum class Request(val url: String) {
        CONNECT("connect"),
        GET_LOCATION("getLocation"),
        ROUTE("route"),
        ROUTE_METRIC("routeMetric"),
        SONAR("sonar"),
        DEBUG_MEMORY("debug/memory");
    }

    fun sendRequest(car: Car, request: Request, data: ByteArray): ListenableFuture<Response>
        = Client.makeRequest("http://${car.host}:${car.port}/${request.url}", data)
}
