/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import com.google.gson.JsonParser
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject.Companion.PACKAGE_JSON
import org.jetbrains.kotlin.gradle.utils.getFile
import java.io.File
import java.net.InetSocketAddress

@DisableCachingByDefault
internal abstract class KotlinSimpleDevServerTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val contentDirectory: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val port: Property<Int>

    @get:Input
    abstract val host: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val importMapFile: RegularFileProperty

    @get:Internal
    abstract val npmRootDirectory: DirectoryProperty

    @TaskAction
    fun start() {
        val rootDir = contentDirectory.get().asFile
        val serverHost = host.get()
        val serverPort = port.getOrElse(findFreePort())

        val importMapModuleDirectories = parseImportMapModuleDirectories()

        val server = HttpServer.create(InetSocketAddress(serverHost, serverPort), 0)
        server.createContext("/") { exchange ->
            handleRequest(
                exchange,
                rootDir,
                npmRootDirectory.getFile(),
                importMapModuleDirectories
            )
        }
        server.executor = null

        server.start()
        logger.lifecycle("Development server started at http://$serverHost:$serverPort")
        logger.lifecycle("Serving files from: $rootDir")
        if (importMapModuleDirectories.isNotEmpty()) {
            logger.lifecycle("Serving ${importMapModuleDirectories.size} node_modules from import map")
        }
        logger.lifecycle("Press Ctrl+C to stop the server")

        try {
            Thread.currentThread().join()
        } catch (_: InterruptedException) {
            server.stop(0)
        }
    }

    /**
     * Parses the import map file and returns a mapping from module name to its directory on disk.
     *
     * The import map contains entries like `"moduleName": "node_modules/moduleName/main.js"`.
     * The paths are relative to [npmRootDirectory].
     */
    private fun parseImportMapModuleDirectories(): Set<File> {
        val mapFile = importMapFile.getFile()

        val npmRoot = npmRootDirectory.getFile()
        val importMapContent = mapFile.readText()
        val importMapObject = JsonParser.parseString(importMapContent).asJsonObject
        val imports = importMapObject.getAsJsonObject("imports") ?: error("No imports in import map $mapFile")

        return imports.entrySet()
            .map { (_, path) ->
                val relativePath = path.asString.trimStart('/')
                val moduleMainFile = npmRoot.resolve(relativePath)
                moduleMainFile.resolveModuleDirectory()
            }.distinct()
            .toSet()
    }

    private fun File.resolveModuleDirectory(): File {
        var packageJsonCandidate = resolveSibling(PACKAGE_JSON)
        while (!packageJsonCandidate.exists()) {
            packageJsonCandidate = packageJsonCandidate.parentFile.resolveSibling(PACKAGE_JSON)
        }

        return packageJsonCandidate.parentFile
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

    /**
     * Resolves the requested path to a file.
     *
     * All from [contentRootDir] are resolved relative to the [contentRootDir] if it exists
     *
     * Requests from [importMapModuleDirectories] are resolved against the import map module directories.
     */
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

    fun findFreePort(startPort: Int = 8080): Int {
        var port = startPort
        while (true) {
            try {
                java.net.ServerSocket(port).use { return port }
            } catch (_: Exception) {
                port++
            }
        }
    }
}
