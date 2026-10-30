/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.file.InvalidPathException

/**
 * Represents a work action for starting and managing a development server
 * that serves static content from explicitly configured directories.
 *
 * The content directory contains only produced artifacts, so it is served as is.
 * The npm root package directory is mounted under its root-relative URL prefix to serve dependencies.
 * The project root directory is served only to make source maps work in a browser,
 * so only Kotlin sources are exposed from it - any other file there may contain secrets.
 * Requests are restricted to local host names and IP addresses to prevent DNS rebinding.
 */
internal abstract class DevServerWorkAction : WorkAction<DevServerWorkAction.DevServerWorkParameters> {

    internal interface DevServerWorkParameters : WorkParameters {
        val contentDirectory: DirectoryProperty
        val npmRootPackageDirectory: DirectoryProperty
        val npmRootPackagePrefix: Property<String>
        val rootDirectory: DirectoryProperty
        val host: Property<String>
        val port: Property<Int>
        val lockFile: RegularFileProperty
        val continuous: Property<Boolean>
    }

    override fun execute() {
        val contentDir = parameters.contentDirectory.getFile()
        val npmRootPackageDir = parameters.npmRootPackageDirectory.getFile()
        val npmRootPackagePrefix = parameters.npmRootPackagePrefix.get()
        val rootDir = parameters.rootDirectory.getFile()
        val serverHost = parameters.host.get()
        val serverPort = parameters.port.get()
        val lockFile = parameters.lockFile.getFile()

        if (parameters.continuous.get()) {
            lockFile.createNewFile()
        }

        val server = startServer(
            contentDir,
            npmRootPackageDir,
            npmRootPackagePrefix,
            rootDir,
            serverHost,
            serverPort,
        )

        val shutdownHook = Thread {
            lockFile.delete()
            server.stop(0)
        }
        Runtime.getRuntime().addShutdownHook(shutdownHook)

        if (parameters.continuous.get()) return

        try {
            Thread.currentThread().join()
        } catch (_: InterruptedException) {
            lockFile.delete()
            server.stop(0)
            Runtime.getRuntime().removeShutdownHook(shutdownHook)
        }
    }

    internal companion object {
        private val logger = Logging.getLogger(DevServerWorkAction::class.java.name)

        /**
         * The only file extension served from the project root directory.
         */
        private const val KOTLIN_SOURCE_EXTENSION = "kt"

        /**
         * Starts a development server on [serverHost] and [serverPort],
         * which serves [contentRootDir], [npmRootPackageDir] under [npmRootPackagePrefix],
         * and Kotlin sources of [rootDir].
         */
        internal fun startServer(
            contentRootDir: File,
            npmRootPackageDir: File,
            npmRootPackagePrefix: String,
            rootDir: File,
            serverHost: String,
            serverPort: Int,
        ): HttpServer {
            val server = HttpServer.create(InetSocketAddress(serverHost, serverPort), 0)
            server.createContext("/") { exchange ->
                handleRequest(
                    exchange,
                    servedDirectories(contentRootDir, npmRootPackageDir, npmRootPackagePrefix, rootDir),
                    serverHost,
                )
            }

            server.executor = null

            server.start()
            if (!server.address.address.isLoopbackAddress) {
                logger.warn(
                    "Development server is bound to non-loopback address ${server.address.address.hostAddress}. " +
                            "Project Kotlin sources will be reachable from the network."
                )
            }
            logger.lifecycle("Development server started at http://$serverHost:$serverPort")
            logger.lifecycle("Serving files from: $contentRootDir")
            logger.lifecycle("Serving npm packages from: $npmRootPackageDir at /${npmRootPackagePrefix.trim('/')}")
            logger.lifecycle("Serving Kotlin sources from: $rootDir")

            return server
        }

        private fun handleRequest(
            exchange: HttpExchange,
            servedDirectories: List<ServedDirectory>,
            serverHost: String,
        ) {
            val hostHeader = exchange.requestHeaders.getFirst("Host")
            if (!isHostAllowed(hostHeader, serverHost)) {
                val rejectedHost = hostHeader ?: "<missing>"
                logger.info("Rejected development server request with Host header '$rejectedHost'")
                respond(exchange, 403, "403 Forbidden: Host '$rejectedHost' is not allowed")
                return
            }

            val path = exchange.requestURI.path.trimStart('/')

            val file = resolveFile(
                path,
                servedDirectories,
            )

            if (file == null || !file.exists() || !file.isFile) {
                respond(exchange, 404, "404 Not Found")
                return
            }

            val contentType = contentTypeFor(file.name)
            val size = file.length()
            exchange.responseHeaders.set("Content-Type", contentType)
            exchange.sendResponseHeaders(200, size)
            exchange.responseBody.use {
                file.inputStream().copyTo(it)
            }
        }

        private fun respond(exchange: HttpExchange, statusCode: Int, message: String) {
            val response = message.toByteArray()
            exchange.responseHeaders.set("Content-Type", "text/plain; charset=utf-8")
            exchange.sendResponseHeaders(statusCode, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }

        private fun contentTypeFor(fileName: String): String = when {
            fileName.endsWith(".html") -> "text/html"
            fileName.endsWith(".kt") -> "text/kotlin; charset=utf-8"
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

        /**
         * Allows hosts that cannot be rebound through DNS, along with the configured server host.
         */
        internal fun isHostAllowed(hostHeader: String?, serverHost: String): Boolean {
            val host = hostHeader?.takeIf { it.isNotBlank() } ?: return false
            val hostName = if (host.startsWith('[')) {
                val closingBracket = host.indexOf(']')
                if (closingBracket == -1) return false

                val suffix = host.substring(closingBracket + 1)
                if (suffix.isNotEmpty() && (!suffix.startsWith(':') || !suffix.drop(1).all { it in '0'..'9' })) {
                    return false
                }

                val address = host.substring(1, closingBracket)
                if (!isIpv6Literal(address)) return false
                address
            } else {
                if (host.count { it == ':' } > 1) return false

                val colonIndex = host.lastIndexOf(':')
                if (colonIndex >= 0) {
                    val port = host.substring(colonIndex + 1)
                    if (port.isEmpty() || !port.all { it in '0'..'9' }) return false
                }
                host.substringBeforeLast(':')
            }

            if (hostName.isEmpty()) return false

            val normalizedServerHost = serverHost.removeSurrounding("[", "]")
            return hostName.equals(normalizedServerHost, ignoreCase = true) ||
                    hostName.equals("localhost", ignoreCase = true) ||
                    isIpLiteral(hostName)
        }

        private fun isIpLiteral(host: String): Boolean {
            if (host.contains(':')) return isIpv6Literal(host)

            val octets = host.split('.')
            return octets.size == 4 && octets.all { octet ->
                octet.isNotEmpty() && octet.all { it.isDigit() } && octet.toIntOrNull()?.let { it in 0..255 } == true
            }
        }

        private fun isIpv6Literal(host: String): Boolean =
            host.contains(':') && runCatching { InetAddress.getByName(host) is Inet6Address }.getOrDefault(false)

        /**
         * Resolves [path] against [servedDirectories] in order, returning the first file allowed by a mount.
         *
         * The mount order ensures that produced artifacts and npm dependencies are served before falling back to
         * Kotlin sources of the project root. Arbitrary files from the project root are not served because they may
         * contain secrets.
         *
         * Returns `null` if the requested file must not be served.
         */
        internal fun resolveFile(
            path: String,
            servedDirectories: List<ServedDirectory>,
        ): File? {
            if (path.isEmpty()) {
                return servedDirectories.firstOrNull()?.resolve("index.html")
            }

            return servedDirectories.firstNotNullOfOrNull { it.resolve(path) }
        }

        internal fun servedDirectories(
            contentRootDir: File,
            npmRootPackageDir: File,
            npmRootPackagePrefix: String,
            rootDir: File,
        ): List<ServedDirectory> = listOf(
            ServedDirectory(contentRootDir),
            ServedDirectory(npmRootPackageDir, npmRootPackagePrefix),
            ServedDirectory(rootDir) { it.extension == KOTLIN_SOURCE_EXTENSION },
        )

        internal class ServedDirectory(
            private val directory: File,
            urlPrefix: String = "",
            private val isServable: (File) -> Boolean = { true },
        ) {
            private val normalizedPrefix = urlPrefix.trim('/')

            fun resolve(requestPath: String): File? {
                if (normalizedPrefix.split('/').any { it == ".." }) return null

                val relativePath = when {
                    normalizedPrefix.isEmpty() -> requestPath
                    requestPath == normalizedPrefix -> ""
                    requestPath.startsWith("$normalizedPrefix/") -> requestPath.removePrefix("$normalizedPrefix/")
                    else -> return null
                }

                val rootPath = directory.toPath().normalize()
                val resolved = try {
                    rootPath.resolve(relativePath).normalize()
                } catch (_: InvalidPathException) {
                    return null
                }

                if (!resolved.startsWith(rootPath)) return null

                return resolved.toFile()
                    .takeIf { it.isFile }
                    ?.takeIf(isServable)
            }
        }
    }
}
