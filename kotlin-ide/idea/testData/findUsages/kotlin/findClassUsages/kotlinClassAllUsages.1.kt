package client

import server.Server

class Client(name: String = Server.NAME): Server() {
    var nextServer: Server? = new Server()
    val name = Server.NAME

    fun foo(s: Server) {
        val server: Server = s
        println("Server: $server")
    }

    fun getNextServer(): Server? {
        return nextServer
    }

    override fun work() {
        super<Server>.work()
        println("Client")
    }

    companion object: Server() {

    }
}

object ClientObject: Server() {

}

class Servers: Iterator<Server> {

}

fun Iterator<Server>.f(p: Iterator<Server>): Iterator<Server> = this

fun Client.bar(s: Server = Server.NAME) {
    foo(s)
}

fun Client.hasNextServer(): Boolean {
    return getNextServer() != null
}

fun Any.asServer(): Server? {
    when (this) {
        is Server -> println("Server!")
    }
    return if (this is Server) this as Server else this as? Server
}