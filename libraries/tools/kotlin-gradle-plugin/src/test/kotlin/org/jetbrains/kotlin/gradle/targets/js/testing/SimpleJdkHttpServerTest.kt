/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.targets.js.testing

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.writeText

class SimpleJdkHttpServerTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var server: SimpleJdkHttpServer

    /**
     * Layout:
     * ```
     * tempDir/
     *   secret.txt            <- must never be served
     *   root/
     *     other.txt           <- must never be served, root itself is not registered
     *     task/
     *       index.html        <- served under TASK_PREFIX
     *       nested/file.js
     *       dir/
     *     build/wasm/
     *       node_modules/foo/index.js  <- served under "build/wasm"
     *     build/wasmFoo/
     *       file.js           <- must not be matched by "build/wasm" prefix
     * ```
     */
    private lateinit var taskDir: Path
    private lateinit var npmRootPackageDir: Path

    @BeforeEach
    fun setUp() {
        tempDir.resolve("secret.txt").writeText("secret")

        val root = tempDir.resolve("root").createDirectories()
        root.resolve("other.txt").writeText("other")

        taskDir = root.resolve("task").createDirectories()
        taskDir.resolve("index.html").writeText("<html>index</html>")
        taskDir.resolve("nested").createDirectories().resolve("file.js").writeText("nested")
        taskDir.resolve("dir").createDirectories()

        npmRootPackageDir = root.resolve("build/wasm").createDirectories()
        npmRootPackageDir.resolve("node_modules/foo").createDirectories().resolve("index.js").writeText("foo")

        root.resolve("build/wasmFoo").createDirectories().resolve("file.js").writeText("wasmFoo")

        server = SimpleJdkHttpServer()
        server.start()
        server.serveStaticFiles(TASK_PREFIX, taskDir)
        server.serveStaticFiles(NPM_PREFIX, npmRootPackageDir)
    }

    @AfterEach
    fun tearDown() {
        server.stopAndClear()
    }

    // region served files

    @Test
    fun `request matching registered prefix is served from prefix directory`() {
        val response = get("/$TASK_PREFIX/index.html")
        assertEquals(200, response.status)
        assertEquals("<html>index</html>", response.body)
        assertEquals("text/html; charset=utf-8", response.contentType)
    }

    @Test
    fun `nested file under registered prefix is served`() {
        val response = get("/$TASK_PREFIX/nested/file.js")
        assertEquals(200, response.status)
        assertEquals("nested", response.body)
    }

    @Test
    fun `request matching additionally registered npm prefix is served`() {
        val response = get("/$NPM_PREFIX/node_modules/foo/index.js")
        assertEquals(200, response.status)
        assertEquals("foo", response.body)
    }

    @Test
    fun `url returned by serveStaticFiles points to registered directory`() {
        val url = server.serveStaticFiles(TASK_PREFIX, taskDir).resolve("index.html")
        val response = get(url.rawPath)
        assertEquals(200, response.status)
        assertEquals("<html>index</html>", response.body)
    }

    // endregion

    // region not found

    @Test
    fun `request matching no prefix is not served`() {
        assertEquals(404, get("/other.txt").status)
        assertEquals(404, get("/root/other.txt").status)
    }

    @Test
    fun `prefix matches only complete path segments`() {
        assertEquals(404, get("/build/wasmFoo/file.js").status)
    }

    @Test
    fun `missing file returns 404`() {
        assertEquals(404, get("/$TASK_PREFIX/missing.js").status)
    }

    @Test
    fun `directory returns 404`() {
        assertEquals(404, get("/$TASK_PREFIX/dir").status)
        assertEquals(404, get("/$TASK_PREFIX/dir/").status)
        assertEquals(404, get("/$TASK_PREFIX/").status)
        assertEquals(404, get("/$TASK_PREFIX").status)
        assertEquals(404, get("/").status)
    }

    // endregion

    // region path traversal

    @Test
    fun `percent encoded path traversal without prefix returns 404`() {
        // JDK decodes %2e to '.' in URI.getPath(), so the request path becomes "/../secret.txt"
        assertEquals(404, get("/%2e%2e/secret.txt").status)
        assertEquals(404, get("/%2e%2e/%2e%2e/secret.txt").status)
    }

    @Test
    fun `path traversal out of prefix directory returns 404`() {
        assertEquals(404, get("/$TASK_PREFIX/../other.txt").status)
        assertEquals(404, get("/$TASK_PREFIX/../../secret.txt").status)
        assertEquals(404, get("/$TASK_PREFIX/../../other.txt").status)
        assertEquals(404, get("/$TASK_PREFIX/nested/../../other.txt").status)
    }

    @Test
    fun `percent encoded path traversal out of prefix directory returns 404`() {
        assertEquals(404, get("/$TASK_PREFIX/%2e%2e/other.txt").status)
        assertEquals(404, get("/$TASK_PREFIX/%2e%2e/%2e%2e/secret.txt").status)
        assertEquals(404, get("/$TASK_PREFIX/%2e%2e%2fother.txt").status)
        assertEquals(404, get("/$TASK_PREFIX/..%2f..%2fsecret.txt").status)
    }

    @Test
    fun `path traversal from npm prefix into sibling directory returns 404`() {
        assertEquals(404, get("/$NPM_PREFIX/../wasmFoo/file.js").status)
        assertEquals(404, get("/$NPM_PREFIX/../../other.txt").status)
    }

    @Test
    fun `backslash path traversal returns 404`() {
        assertEquals(404, get("/$TASK_PREFIX/..%5c..%5csecret.txt").status)
    }

    @Test
    fun `absolute path after prefix returns 404`() {
        val secret = tempDir.resolve("secret.txt").toAbsolutePath().invariantSeparatorsPathString.trimStart('/')
        assertEquals(404, get("/$TASK_PREFIX//$secret").status)
    }

    @Test
    fun `windows absolute path returns 404`() {
        val secret = tempDir.resolve("secret.txt").toAbsolutePath()
        // Only on Windows the path has a drive letter, e.g. C:/Users/.../secret.txt
        assumeTrue(secret.root?.toString()?.contains(':') == true, "Windows-only test")

        val secretPath = secret.invariantSeparatorsPathString
        assertEquals(404, get("/$secretPath").status)
        assertEquals(404, get("/$TASK_PREFIX/$secretPath").status)
    }

    @Test
    fun `windows drive path is not served on any platform`() {
        assertEquals(404, get("/C:/Windows/win.ini").status)
        assertEquals(404, get("/$TASK_PREFIX/C:/Windows/win.ini").status)
        assertEquals(404, get("/$TASK_PREFIX/C:%5cWindows%5cwin.ini").status)
    }

    @Test
    fun `null byte in path returns 404`() {
        assertEquals(404, get("/$TASK_PREFIX/index.html%00.js").status)
    }

    // endregion

    private class Response(val status: Int, val contentType: String?, val body: String)

    /**
     * Sends GET request for [rawPath] as is.
     * [HttpClient] doesn't normalize the request [URI], so `..` segments reach the server exactly as written.
     * Don't use [URI.resolve] here, as it normalizes `..` segments.
     */
    private fun get(rawPath: String): Response {
        val request = HttpRequest.newBuilder(URI(server.serverAddress.removeSuffix("/") + rawPath)).GET().build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        return Response(
            status = response.statusCode(),
            contentType = response.headers().firstValue("Content-Type").orElse(null),
            body = response.body(),
        )
    }

    private companion object {
        const val TASK_PREFIX = ":app:wasmJsTestBundleAsEsm"
        const val NPM_PREFIX = "build/wasm"

        val httpClient: HttpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build()
    }
}
