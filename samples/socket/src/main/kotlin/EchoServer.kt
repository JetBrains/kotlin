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
import platform.posix.*

fun main(args: Array<String>) {
    if (args.size < 1) {
        println("Usage: ./echo_server <port>")
        return
    }

    val port = args[0].toShort()

    // Initialize sockets in platform-dependent way.
    init_sockets()

    memScoped {

        val buffer = ByteArray(1024)
        val prefixBuffer = "echo: ".toUtf8()
        val serverAddr = alloc<sockaddr_in>()

        val listenFd = socket(AF_INET, SOCK_STREAM, 0)
                .ensureUnixCallResult("socket") { it >= 0 }

        with(serverAddr) {
            memset(this.ptr, 0, sockaddr_in.size)
            sin_family = AF_INET.narrow()
            sin_port = posix_htons(port)
        }

        bind(listenFd, serverAddr.ptr.reinterpret(), sockaddr_in.size.toInt())
                .ensureUnixCallResult("bind") { it == 0 }

        listen(listenFd, 10)
                .ensureUnixCallResult("listen") { it == 0 }

        val commFd = accept(listenFd, null, null)
                .ensureUnixCallResult("accept") { it >= 0 }

        buffer.usePinned { pinned ->
          while (true) {
            val length = recv(commFd, pinned.addressOf(0), buffer.size.signExtend(), 0).toInt()
                    .ensureUnixCallResult("read") { it >= 0 }

            if (length == 0) {
                break
            }

            send(commFd, prefixBuffer.refTo(0), prefixBuffer.size.signExtend(), 0)
                    .ensureUnixCallResult("write") { it >= 0 }
            send(commFd, pinned.addressOf(0), length.signExtend(), 0)
                    .ensureUnixCallResult("write") { it >= 0 }
          }
        }
    }
}

inline fun Int.ensureUnixCallResult(op: String, predicate: (Int) -> Boolean): Int {
    if (!predicate(this)) {
        throw Error("$op: ${strerror(posix_errno())!!.toKString()}")
    }
    return this
}

inline fun Long.ensureUnixCallResult(op: String, predicate: (Long) -> Boolean): Long {
    if (!predicate(this)) {
        throw Error("$op: ${strerror(posix_errno())!!.toKString()}")
    }
    return this
}
