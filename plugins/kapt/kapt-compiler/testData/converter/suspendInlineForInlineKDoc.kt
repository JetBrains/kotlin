// WITH_STDLIB

class Request(val value: String)

interface Client {
    suspend fun send(request: Request): String
}

/**
 * Sends a request built by [block].
 */
suspend inline fun Client.send(crossinline block: () -> String): String = send(Request(block()))
