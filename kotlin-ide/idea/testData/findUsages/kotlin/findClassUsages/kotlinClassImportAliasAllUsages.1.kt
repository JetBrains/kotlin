package server

open class Server {
    companion object {
        val NAME = "Server"
    }

    open fun work() {
        println("Server")
    }
}
