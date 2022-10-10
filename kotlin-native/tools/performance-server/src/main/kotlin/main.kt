/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.network.*

external fun require(module: String): dynamic

external val process: dynamic
external val __dirname: dynamic

fun main() {
    println("Server Starting!")

    val express = require("express")
    val app = express()
    val path = require("path")
    val bodyParser = require("body-parser")
    val http = require("http")
    // Get port from environment and store in Express.
    val port = normalizePort(process.env.PORT)
    app.use(bodyParser.json())
    app.set("port", port)

    // View engine setup.
    app.set("views", path.join(__dirname, "../ui"))
    app.set("view engine", "ejs")
    app.use(express.static("ui"))

    http.createServer(app)
    app.listen(port, {
        println("App listening on port " + port + "!")
    })

    val connector = if (process.env.LOCAL_AWS != null && process.env.LOCAL_AWS != kotlin.js.undefined) {
        println("Using local aws instance")
        UrlNetworkConnector("http://localhost", 9200)
    } else {
        val host = process.env.AWS_HOST
        val region = process.env.AWS_REGION
        if (host !is String) throw IllegalStateException("AWS_HOST env variable is not defined")
        if (region !is String) throw IllegalStateException("AWS_REGION env variable is not defined")
        AWSNetworkConnector(
                host, region
        )
    }

    app.use("/", router(connector))
}

fun normalizePort(port: Int) =
    if (port >= 0) port else 3000