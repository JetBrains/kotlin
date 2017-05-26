/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import kotlinx.cinterop.*
import sockets.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

fun main(args: Array<String>) {
    if (args.size < 1) {
        println("Usage: ./echo_server <port>")
        return
    }

    val port = args[0].toShort()

    memScoped {

        val serverAddr = alloc<sockaddr_in>()

        val listenFd = socket(AF_INET, SOCK_STREAM, 0)
                .ensureUnixCallResult { it >= 0 }

        with(serverAddr) {
            memset(this.ptr, 0, sockaddr_in.size)
            sin_family = AF_INET.narrow()
            sin_addr.s_addr = htons(0).toInt()
            sin_port = htons(port)
        }

        bind(listenFd, serverAddr.ptr.reinterpret(), sockaddr_in.size.toInt())
                .ensureUnixCallResult { it == 0 }

        fcntl(listenFd, F_SETFL, O_NONBLOCK)
                .ensureUnixCallResult { it == 0 }

        listen(listenFd, 10)
                .ensureUnixCallResult { it == 0 }

        var connectionId = 0
        acceptClientsAndRun(listenFd) {
            memScoped {
                val bufferLength = 100L
                val buffer = allocArray<ByteVar>(bufferLength)
                val connectionIdString = "#${++connectionId}: ".cstr
                val connectionIdBytes = connectionIdString.getPointer(this)

                try {
                    while (true) {
                        val length = read(buffer, bufferLength)

                        if (length == 0L)
                            break

                        write(connectionIdBytes, connectionIdString.size.toLong())
                        write(buffer, length)
                    }
                } catch (e: IOException) {
                    println("I/O error occured: ${e.message}")
                }
            }
        }
    }
}

sealed class WaitingFor {
    class Accept : WaitingFor()

    class Read(val data: CArrayPointer<ByteVar>,
               val length: Long,
               val continuation: Continuation<Long>) : WaitingFor()

    class Write(val data: CArrayPointer<ByteVar>,
                val length: Long,
                val continuation: Continuation<Unit>) : WaitingFor()
}

class Client(val clientFd: Int, val waitingList: MutableMap<Int, WaitingFor>) {
    suspend fun read(data: CArrayPointer<ByteVar>, dataLength: Long): Long {
        val length = read(clientFd, data, dataLength)
        if (length >= 0)
            return length
        if (errno != EWOULDBLOCK)
            throw IOException(getUnixError())
        // Save continuation and suspend.
        return suspendCoroutineOrReturn { continuation ->
            waitingList.put(clientFd, WaitingFor.Read(data, dataLength, continuation))
            COROUTINE_SUSPENDED
        }
    }

    suspend fun write(data: CArrayPointer<ByteVar>, length: Long) {
        val written = write(clientFd, data, length)
        if (written >= 0)
            return
        if (errno != EWOULDBLOCK)
            throw IOException(getUnixError())
        // Save continuation and suspend.
        return suspendCoroutineOrReturn { continuation ->
            waitingList.put(clientFd, WaitingFor.Write(data, length, continuation))
            COROUTINE_SUSPENDED
        }
    }
}

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resume(value: Any?) {}
    override fun resumeWithException(exception: Throwable) { throw exception }
}

fun acceptClientsAndRun(serverFd: Int, block: suspend Client.() -> Unit) {
    memScoped {
        val waitingList = mutableMapOf<Int, WaitingFor>(serverFd to WaitingFor.Accept())
        val readfds = alloc<fd_set>()
        val writefds = alloc<fd_set>()
        val errorfds = alloc<fd_set>()
        var maxfd = serverFd
        while (true) {
            FD_ZERO(readfds)
            FD_ZERO(writefds)
            FD_ZERO(errorfds)
            for ((socketFd, watingFor) in waitingList) {
                when (watingFor) {
                    is WaitingFor.Accept -> FD_SET(socketFd, readfds)
                    is WaitingFor.Read   -> FD_SET(socketFd, readfds)
                    is WaitingFor.Write  -> FD_SET(socketFd, writefds)
                }
                FD_SET(socketFd, errorfds)
            }
            pselect(maxfd + 1, readfds.ptr, writefds.ptr, errorfds.ptr, null, null)
                    .ensureUnixCallResult { it >= 0 }
            loop@for (socketFd in 0..maxfd) {
                val waitingFor = waitingList[socketFd]
                val errorOccured = FD_ISSET(socketFd, errorfds)
                if (FD_ISSET(socketFd, readfds) || FD_ISSET(socketFd, writefds) || errorOccured) {
                    when (waitingFor) {
                        is WaitingFor.Accept -> {
                            if (errorOccured)
                                throw Error("Socket has been closed externally")

                            // Accept new client.
                            val clientFd = accept(serverFd, null, null)
                            if (clientFd < 0) {
                                if (errno != EWOULDBLOCK)
                                    throw Error(getUnixError())
                                break@loop
                            }
                            fcntl(clientFd, F_SETFL, O_NONBLOCK)
                                    .ensureUnixCallResult { it == 0 }
                            if (maxfd < clientFd)
                                maxfd = clientFd
                            block.startCoroutine(Client(clientFd, waitingList), EmptyContinuation)
                        }
                        is WaitingFor.Read -> {
                            if (errorOccured)
                                waitingFor.continuation.resumeWithException(IOException("Connection was closed by peer"))

                            // Resume reading operation.
                            waitingList.remove(socketFd)
                            val length = read(socketFd, waitingFor.data, waitingFor.length)
                            if (length < 0) // Read error.
                                waitingFor.continuation.resumeWithException(IOException(getUnixError()))
                            waitingFor.continuation.resume(length)
                        }
                        is WaitingFor.Write -> {
                            if (errorOccured)
                                waitingFor.continuation.resumeWithException(IOException("Connection was closed by peer"))

                            // Resume writing operation.
                            waitingList.remove(socketFd)
                            val written = write(socketFd, waitingFor.data, waitingFor.length)
                            if (written < 0) // Write error.
                                waitingFor.continuation.resumeWithException(IOException(getUnixError()))
                            waitingFor.continuation.resume(Unit)
                        }
                    }
                }
            }
        }
    }
}

class IOException(message: String): RuntimeException(message)

val errno: Int
    get() = interop_errno()

fun FD_ZERO(set: fd_set): Unit = interop_FD_ZERO(set.ptr)

fun FD_SET(bit: Int, set: fd_set): Unit = interop_FD_SET(bit, set.ptr)

fun FD_ISSET(bit: Int, set: fd_set) = interop_FD_ISSET(bit, set.ptr) != 0

fun htons(value: Short) = interop_htons(value.toInt()).toShort()

fun getUnixError() = strerror(errno)!!.toKString()

inline fun Int.ensureUnixCallResult(predicate: (Int) -> Boolean): Int {
    if (!predicate(this)) {
        throw Error(getUnixError())
    }
    return this
}

inline fun Long.ensureUnixCallResult(predicate: (Long) -> Boolean): Long {
    if (!predicate(this)) {
        throw Error(getUnixError())
    }
    return this
}