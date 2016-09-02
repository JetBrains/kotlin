package net.car.client

import org.asynchttpclient.DefaultAsyncHttpClient
import org.asynchttpclient.ListenableFuture
import org.asynchttpclient.Response
import java.rmi.UnexpectedException

object Client {

    private val client = DefaultAsyncHttpClient()
    private val timeout = 5 * 60 * 60 * 1000

    enum class Request(val url: String) {
        CONNECT("connect"),
        GET_LOCATION("getLocation"),
        ROUTE("route"),
        ROUTE_METRIC("routeMetric"),
        SONAR("sonar"),
        DEBUG_MEMORY("debug/memory"),
        DEBUG_SONAR("debug/sonar"),
        PING("ping"),
        EXPLORE_ANGLE("sonarExplore");
    }

    fun shutDownClient() {
        client.close()
    }

    fun makeRequest(request: String, data: ByteArray): ListenableFuture<Response> =
            client.preparePost(request).setBody(data).setRequestTimeout(timeout).execute() ?: throw UnexpectedException(request)

}
