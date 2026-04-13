/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import java.net.InetSocketAddress

internal abstract class DevServerWorkAction : WorkAction<DevServerWorkAction.DevServerWorkParameters> {

    internal interface DevServerWorkParameters : WorkParameters {
        val contentDirectory: DirectoryProperty
        val host: Property<String>
        val port: Property<Int>
        val npmRootDirectory: DirectoryProperty
        val importMapModuleDirectories: SetProperty<File>
    }

    private val logger = Logging.getLogger(DevServerWorkAction::class.java.name)

    override fun execute() {
        val rootDir = parameters.contentDirectory.getFile()
        val serverHost = parameters.host.get()
        val serverPort = parameters.port.get()
        val npmRootDir = parameters.npmRootDirectory.getFile()
        val importMapModuleDirectories = parameters.importMapModuleDirectories.get()

        val server = HttpServer.create(InetSocketAddress(serverHost, serverPort), 0)
        server.createContext("/") { exchange ->
            handleRequest(
                exchange,
                rootDir,
                npmRootDir,
                importMapModuleDirectories
            )
        }

        server.executor = null

        val shutdownHook = Thread {
            server.stop(0)
        }
        Runtime.getRuntime().addShutdownHook(shutdownHook)

        server.start()
        logger.lifecycle("Development server started at http://$serverHost:$serverPort")
        logger.lifecycle("Serving files from: $rootDir")
        if (importMapModuleDirectories.isNotEmpty()) {
            logger.lifecycle("Serving ${importMapModuleDirectories.size} node_modules from import map")
        }

        try {
            Thread.currentThread().join()
        } catch (_: InterruptedException) {
            server.stop(0)
            Runtime.getRuntime().removeShutdownHook(shutdownHook)
        }
    }

    private fun handleRequest(
        exchange: HttpExchange,
        contentRootDir: File,
        npmRootDir: File,
        importMapModuleDirectories: Set<File>,
    ) {
        val path = exchange.requestURI.path.trimStart('/')

        val file = resolveFile(
            path,
            contentRootDir,
            npmRootDir,
            importMapModuleDirectories
        )

        if (file == null || !file.exists() || !file.isFile) {
            val notFound = "404 Not Found".toByteArray()
            exchange.responseHeaders.set("Content-Type", "text/plain")
            exchange.sendResponseHeaders(404, notFound.size.toLong())
            exchange.responseBody.use { it.write(notFound) }
            return
        }

        val contentType = contentTypeFor(file.name)
        val bytes = file.readBytes()
        exchange.responseHeaders.set("Content-Type", contentType)
        addCorsHeaders(exchange)
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun resolveFile(
        path: String,
        contentRootDir: File,
        npmRootDir: File,
        importMapModuleDirectories: Set<File>,
    ): File? {
        if (path.isEmpty()) {
            return File(contentRootDir, "index.html")
        }

        contentRootDir.resolve(path)
            .takeIf { it.normalize().startsWith(contentRootDir.normalize()) }
            ?.takeIf { it.exists() }
            ?.let { return it }

        importMapModuleDirectories.forEach { moduleDir ->
            val resolvedFile = npmRootDir.resolve(path)
            if (resolvedFile.startsWith(moduleDir)) return resolvedFile
        }

        return null
    }

    private fun addCorsHeaders(exchange: HttpExchange) {
        exchange.responseHeaders.set("Cross-Origin-Opener-Policy", "same-origin")
        exchange.responseHeaders.set("Cross-Origin-Embedder-Policy", "require-corp")
    }

    private fun contentTypeFor(fileName: String): String = when {
        fileName.endsWith(".html") -> "text/html"
        fileName.endsWith(".js") -> "application/javascript"
        fileName.endsWith(".mjs") -> "application/javascript"
        fileName.endsWith(".wasm") -> "application/wasm"
        fileName.endsWith(".css") -> "text/css"
        fileName.endsWith(".json") -> "application/json"
        fileName.endsWith(".png") -> "image/png"
        fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") -> "image/jpeg"
        fileName.endsWith(".svg") -> "image/svg+xml"
        fileName.endsWith(".ico") -> "image/x-icon"
        fileName.endsWith(".map") -> "application/json"
        else -> "application/octet-stream"
    }
}
