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

fun main(args: Array<String>) {
    if (args.size < 1) {
        println("Usage: ./echo_server <port>")
        return
    }

    val port = args[0].toShort()

    memScoped {

        val bufferLength = 100L
        val buffer = allocArray<ByteVar>(bufferLength)
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

        listen(listenFd, 10)
                .ensureUnixCallResult { it == 0 }

        val commFd = accept(listenFd, null, null)
                .ensureUnixCallResult { it >= 0 }

        while (true) {
            val length = read(commFd, buffer, bufferLength)
                    .ensureUnixCallResult { it >= 0 }

            if (length == 0L) {
                break
            }

            write(commFd, buffer, length)
                    .ensureUnixCallResult { it >= 0 }
        }
    }
}

val errno: Int
    get() = interop_errno()

fun htons(value: Short) = interop_htons(value.toInt()).toShort()

inline fun Int.ensureUnixCallResult(predicate: (Int) -> Boolean): Int {
    if (!predicate(this)) {
        throw Error(strerror(errno)!!.toKString())
    }
    return this
}

inline fun Long.ensureUnixCallResult(predicate: (Long) -> Boolean): Long {
    if (!predicate(this)) {
        throw Error(strerror(errno)!!.toKString())
    }
    return this
}