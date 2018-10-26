/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("HttpServer")

package sample.html5canvas.httpserver

import io.ktor.http.content.default
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.io.File

fun main(args: Array<String>) {

    check(args.size == 1) { "Invalid number of arguments: $args.\nExpected one argument with content root." }

    val contentRoot = File(args[0])
    check(contentRoot.isDirectory) { "Invalid content root: $contentRoot." }

    println(
            """

                IMPORTANT: Please open http://localhost:8080/ in your browser!

                To stop embedded HTTP server use Ctrl+C (Cmd+C for Mac OS X).

            """.trimIndent()
    )

    val server = embeddedServer(Netty, 8080) {
        routing {
            static("/") {
                files(contentRoot)
                default("index.html")
            }
        }
    }
    server.start(wait = true)
}
