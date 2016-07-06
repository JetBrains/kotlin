import Server.Server

/**
 * Created by user on 7/6/16.
 */

val port:Int = 7925;

fun main(args: Array<String>) {
    println("server started")
    val server = Server(port)
    val serverThread = Thread(server);
    serverThread.start()
}