/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("HttpServer")

package sample.html5canvas.httpserver

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.content.LocalFileContent
import io.ktor.http.content.default
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.response.respond
import io.ktor.routing.get
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
            val wasm = "build/bin/html5Canvas/releaseExecutable/html5Canvas.wasm"
            get(wasm) {
                // TODO: ktor as of now doesn't know about 'application/wasm'.
                // The newer browsers (firefox and chrome at least) don't allow
                // 'application/octet-stream' for wasm anymore.
                // We provide the proper content type here and,
                // at the same time, put it into the ktor database.
                // Remove this whole get() clause when ktor fix is available.
                call.respond(LocalFileContent(File(wasm), ContentType("application", "wasm")))
            }
            static("/") {
                files(contentRoot)
                default("index.html")
            }
        }
    }
    server.start(wait = true)
}
