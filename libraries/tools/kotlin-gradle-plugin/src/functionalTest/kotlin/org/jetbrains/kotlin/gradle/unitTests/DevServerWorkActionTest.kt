/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.targets.js.ir.DevServerWorkAction
import org.jetbrains.kotlin.gradle.util.assertLogContains
import org.jetbrains.kotlin.gradle.util.assertLogDoesNotContain
import org.jetbrains.kotlin.gradle.util.withGradleLogCapture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.InetSocketAddress
import java.net.ProxySelector
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * The development server serves two directories:
 * the content directory with the produced artifacts and the project root directory.
 *
 * The project root directory is served only to make source maps work in a browser,
 * so only Kotlin sources must be available from it - any other file there may contain secrets.
 *
 * Requests of other sites are refused by the `Host` header check, which prevents DNS rebinding.
 */
class DevServerWorkActionTest {

    @Test
    fun `serves the content directory and Kotlin sources of the project root`(
        @TempDir
        tempDir: File,
    ) {
        val project = project(tempDir)

        val responses = project.withDevServer { port ->
            listOf(
                "/",
                "/index.html",
                "/main.wasm",
                "/src/wasmJsMain/kotlin/A.kt",
                "/build/dist/../../src/wasmJsMain/kotlin/A.kt",
            ).associateWith { httpGet(port, it) }
        }

        assertEquals(
            responses.keys.associateWith { 200 },
            responses.mapValues { (_, response) -> response.status },
        )

        assertEquals(INDEX_HTML, responses.getValue("/").body)
        assertEquals(INDEX_HTML, responses.getValue("/index.html").body)

        val servedSource = responses.getValue("/src/wasmJsMain/kotlin/A.kt")
        assertEquals(MAIN_SOURCE, servedSource.body)
        assertEquals("text/kotlin; charset=utf-8", servedSource.contentType)
    }

    @Test
    fun `refuses files of the project root which are not Kotlin sources`(
        @TempDir
        tempDir: File,
    ) {
        val project = project(tempDir)

        val responses = project.withDevServer { port ->
            listOf(
                "/local.properties",
                "/.env",
                "/src/wasmJsMain/resources/index.html",
                "/src/wasmJsMain/kotlin",
                "/../outside.kt",
                "/%2e%2e/outside.kt",
                "/build/dist/../../../outside.kt",
                "//" + project.rootDir.resolve("local.properties").invariantSeparatorsPath.trimStart('/'),
            ).associateWith { httpGet(port, it) }
        }

        assertEquals(
            responses.keys.associateWith { 404 },
            responses.mapValues { (_, response) -> response.status },
        )
    }

    @Test
    fun `refuses a path which is not a valid file path`(
        @TempDir
        tempDir: File,
    ) {
        val project = project(tempDir)

        // such a path can't be requested over HTTP, but it must not break the resolution either
        assertNull(
            DevServerWorkAction.resolveFile(
                "src/wasmJsMain/kotlin/A\u0000.kt",
                project.contentDir,
                project.rootDir,
            )
        )
    }

    @Test
    fun `refuses requests with a Host header of another site`(
        @TempDir
        tempDir: File,
    ) {
        val project = project(tempDir)

        val responses = project.withDevServer { port ->
            mapOf(
                // the server host, with and without the port
                "localhost:$port" to 200,
                "localhost" to 200,
                // IP literals can't be rebound through DNS
                "127.0.0.1:$port" to 200,
                "[::1]:$port" to 200,
                // another site tries to read the project sources after DNS rebinding
                "evil.example" to 403,
                "evil.example:$port" to 403,
            ).mapValues { (hostHeader, expectedStatus) ->
                expectedStatus to httpGet(port, "/", hostHeader)
            }
        }

        assertEquals(
            responses.mapValues { (_, response) -> response.first },
            responses.mapValues { (_, response) -> response.second.status },
        )

        assertEquals("403 Forbidden: Host 'evil.example' is not allowed", responses.getValue("evil.example").second.body)
    }

    @Test
    fun `refuses requests without a usable Host header`() {
        // such requests can't be sent with HttpClient, which always sets the header to the authority of the request URI
        assertFalse(DevServerWorkAction.isHostAllowed(null, SERVER_HOST))
        assertFalse(DevServerWorkAction.isHostAllowed("", SERVER_HOST))
    }

    @Test
    fun `warns when the server is bound to a non-loopback address`(
        @TempDir
        tempDir: File,
    ) {
        val project = project(tempDir)

        val nonLoopbackLogs = withGradleLogCapture {
            project.withDevServer(host = "0.0.0.0") {}
        }
        nonLoopbackLogs.assertLogContains(NON_LOOPBACK_WARNING)

        val loopbackLogs = withGradleLogCapture {
            project.withDevServer {}
        }
        loopbackLogs.assertLogDoesNotContain(NON_LOOPBACK_WARNING)
    }

    private class DevServerProject(val contentDir: File, val rootDir: File)

    private fun project(tempDir: File): DevServerProject {
        val rootDir = tempDir.resolve("project")

        val contentDir = rootDir.resolve("build/dist")
        contentDir.mkdirs()
        contentDir.resolve("index.html").writeText(INDEX_HTML)
        contentDir.resolve("main.wasm").writeText("wasm binary")

        rootDir.resolve("src/wasmJsMain/kotlin").mkdirs()
        rootDir.resolve("src/wasmJsMain/kotlin/A.kt").writeText(MAIN_SOURCE)
        rootDir.resolve("src/wasmJsMain/resources").mkdirs()
        rootDir.resolve("src/wasmJsMain/resources/index.html").writeText(INDEX_HTML)

        rootDir.resolve("local.properties").writeText("password=hunter2")
        rootDir.resolve(".env").writeText("TOKEN=hunter2")

        // a Kotlin source outside of the project root, only the containment check can refuse it
        tempDir.resolve("outside.kt").writeText("fun outside() {}")

        return DevServerProject(contentDir, rootDir)
    }

    private fun <T> DevServerProject.withDevServer(
        host: String = SERVER_HOST,
        action: (port: Int) -> T,
    ): T {
        val server = DevServerWorkAction.startServer(contentDir, rootDir, host, 0)
        return try {
            action(server.address.port)
        } finally {
            server.stop(0)
        }
    }

    private class DevServerResponse(val status: Int, val contentType: String?, val body: String)

    /**
     * Sends a GET request for [rawPath] with [hostHeader] as the `Host` header.
     *
     * [HttpClient] forbids setting this header and always derives it from the authority of the request URI,
     * so the server itself is used as an HTTP proxy:
     * the request is sent to [port], while its URI only provides [hostHeader] and [rawPath].
     *
     * [HttpClient] doesn't normalize the request [URI], so `..` segments reach the server exactly as written.
     * Don't use [URI.resolve] here, as it normalizes `..` segments.
     */
    private fun httpGet(port: Int, rawPath: String, hostHeader: String = "$SERVER_HOST:$port"): DevServerResponse {
        val client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .proxy(ProxySelector.of(InetSocketAddress(SERVER_HOST, port)))
            .build()

        val request = HttpRequest.newBuilder(URI("http://$hostHeader$rawPath")).GET().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        return DevServerResponse(
            status = response.statusCode(),
            contentType = response.headers().firstValue("Content-Type").orElse(null),
            body = response.body(),
        )
    }

    private companion object {
        const val INDEX_HTML = "<html><body>Hello, world</body></html>"
        const val MAIN_SOURCE = "fun main() {\n    println(\"Hello, world\")\n}"

        const val SERVER_HOST = "localhost"

        const val NON_LOOPBACK_WARNING = "Development server is bound to non-loopback address"
    }
}
