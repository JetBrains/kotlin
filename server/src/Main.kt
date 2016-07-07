import server.Server
import java.awt.Robot
import java.util.*
import kotlin.concurrent.thread
import kotlin.jvm.internal.iterator

/**
 * Created by user on 7/6/16.
 */

//хардкод это плохо, но пока так...:)
val port:Int = 7925
val handlerThreadsCount:Int = 100
val getLocationUrl = "getLocation"
val routeDoneUrl = "routeDone"
val setRouteUrl = "route"
val connectUrl = "connect"

fun main(args: Array<String>) {
    println("server started")
    val server = Server(port, handlerThreadsCount)
    val serverThread = thread{server.run()}
}