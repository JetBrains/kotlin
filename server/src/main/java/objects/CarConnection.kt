package objects

import net.car.client.Client
import org.asynchttpclient.ListenableFuture
import org.asynchttpclient.Response


class CarConnection(val host: String, val port: Int) {

    fun sendRequest(request: Client.Request, data: ByteArray): ListenableFuture<Response> {
        return Client.makeRequest("http://$host:$port/${request.url}", data)
    }

}