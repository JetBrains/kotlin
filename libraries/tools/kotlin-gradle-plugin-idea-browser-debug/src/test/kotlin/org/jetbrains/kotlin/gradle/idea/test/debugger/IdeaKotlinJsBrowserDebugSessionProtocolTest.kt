/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.test.debugger

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.jetbrains.kotlin.gradle.idea.debugger.*
import org.jetbrains.kotlin.gradle.idea.debugger.IdeaKotlinJsBrowserDebugSession.ConnectionAborted
import org.jetbrains.kotlin.gradle.idea.debugger.IdeaKotlinJsBrowserDebugSessionProtocol.ABORT_PATH
import org.jetbrains.kotlin.gradle.idea.debugger.IdeaKotlinJsBrowserDebugSessionProtocol.BASE_PATH
import org.jetbrains.kotlin.gradle.idea.debugger.IdeaKotlinJsBrowserDebugSessionProtocol.DEBUGGABLE_BROWSER_READY_PATH
import org.jetbrains.kotlin.gradle.idea.debugger.IdeaKotlinJsBrowserDebugSessionProtocol.DEBUGGER_STATE_PATH
import org.jetbrains.kotlin.gradle.idea.debugger.IdeaKotlinJsBrowserDebugSessionProtocol.FINISH_PATH
import org.jetbrains.kotlin.gradle.idea.debugger.IdeaKotlinJsBrowserDebugSessionProtocol.json
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URL
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Pins the JSON wire format and the HTTP shape of [IdeaKotlinJsBrowserDebugSession].
 *
 * The IDE and the build system sides of a session are not necessarily of the same version, so these
 * expectations must not change silently: see `IdeaKotlinJsBrowserDebugSessionProtocol`.
 */
class IdeaKotlinJsBrowserDebugSessionProtocolTest {
    @Test
    fun `test - encoded messages`() {
        assertEquals(
            """{"cdpUrl":"http://127.0.0.1:9222"}""",
            json.encodeToString(
                IdeaKotlinDebuggableBrowserMessage.serializer(),
                IdeaKotlinDebuggableBrowserMessage("http://127.0.0.1:9222")
            )
        )

        assertEquals(
            """{"reason":"boom"}""",
            json.encodeToString(IdeaKotlinAbortSessionMessage.serializer(), IdeaKotlinAbortSessionMessage("boom"))
        )

        assertEquals(
            """{"acknowledged":true}""",
            json.encodeToString(IdeaKotlinAcknowledgedMessage.serializer(), IdeaKotlinAcknowledgedMessage())
        )

        assertEquals(
            """{"state":"WAITING_FOR_DEBUGGER","reason":null}""",
            json.encodeToString(
                IdeaKotlinDebuggerStateMessage.serializer(),
                IdeaKotlinDebuggerStateMessage(IdeaKotlinDebuggerState.WAITING_FOR_DEBUGGER)
            )
        )

        assertEquals(
            """{"state":"ABORTED","reason":"boom"}""",
            json.encodeToString(
                IdeaKotlinDebuggerStateMessage.serializer(),
                IdeaKotlinDebuggerStateMessage(IdeaKotlinDebuggerState.ABORTED, "boom")
            )
        )

        assertEquals(
            """{"error":"boom"}""",
            json.encodeToString(IdeaKotlinSessionErrorMessage.serializer(), IdeaKotlinSessionErrorMessage("boom"))
        )
    }

    @Test
    fun `test - requests sent by the build system`() {
        val browser = IdeaKotlinDebuggableBrowser(cdpUrl = "http://127.0.0.1:9222")

        withMockIde(
            respond = { exchange ->
                when (exchange.requestURI.path) {
                    DEBUGGER_STATE_PATH -> """{"state":"DEBUGGER_READY"}"""
                    else -> """{"acknowledged":true}"""
                }
            }
        ) { mockIde ->
            val buildSystem = IdeaKotlinJsBrowserDebugSessionClient(mockIde.connectionUrl)
            buildSystem.sendBrowserReady(browser)
            buildSystem.awaitDebuggerReady(browser, timeout = 10.seconds)
            buildSystem.sendFinished(browser)
            buildSystem.abort("boom")

            assertEquals(
                listOf(
                    RecordedRequest("POST", DEBUGGABLE_BROWSER_READY_PATH, null, """{"cdpUrl":"http://127.0.0.1:9222"}"""),
                    RecordedRequest("GET", DEBUGGER_STATE_PATH, "cdpUrl=http%3A%2F%2F127.0.0.1%3A9222", ""),
                    RecordedRequest("POST", FINISH_PATH, null, """{"cdpUrl":"http://127.0.0.1:9222"}"""),
                    RecordedRequest("POST", ABORT_PATH, null, """{"reason":"boom"}"""),
                ),
                mockIde.requests
            )
        }
    }

    @Test
    fun `test - the build system keeps polling until the debugger is ready`() {
        val browser = IdeaKotlinDebuggableBrowser(cdpUrl = "http://127.0.0.1:9222")
        var polls = 0

        withMockIde(
            respond = { exchange ->
                when (exchange.requestURI.path) {
                    DEBUGGER_STATE_PATH -> if (++polls < 3) """{"state":"WAITING_FOR_DEBUGGER"}""" else """{"state":"DEBUGGER_READY"}"""
                    else -> """{"acknowledged":true}"""
                }
            }
        ) { mockIde ->
            IdeaKotlinJsBrowserDebugSessionClient(mockIde.connectionUrl, pollingInterval = 10.milliseconds)
                .awaitDebuggerReady(browser, timeout = 10.seconds)

            assertEquals(3, polls)
        }
    }

    @Test
    fun `test - a malformed response is reported as an aborted connection`() {
        val browser = IdeaKotlinDebuggableBrowser(cdpUrl = "http://127.0.0.1:9222")

        withMockIde(respond = { "this is not JSON" }) { mockIde ->
            val buildSystem = IdeaKotlinJsBrowserDebugSessionClient(mockIde.connectionUrl)
            val failure = assertFailsWith<ConnectionAborted> { buildSystem.sendBrowserReady(browser) }
            assertTrue(
                "this is not JSON" in failure.message.orEmpty(),
                "Expected the unexpected response to be reported, but got '${failure.message}'"
            )
        }
    }

    @Test
    fun `test - an error response is reported as an aborted connection`() {
        val browser = IdeaKotlinDebuggableBrowser(cdpUrl = "http://127.0.0.1:9222")

        withMockIde(
            statusCode = HttpURLConnection.HTTP_CONFLICT,
            respond = { """{"error":"the IDE is busy"}""" }
        ) { mockIde ->
            val buildSystem = IdeaKotlinJsBrowserDebugSessionClient(mockIde.connectionUrl)
            val failure = assertFailsWith<ConnectionAborted> { buildSystem.sendBrowserReady(browser) }
            assertTrue(
                "the IDE is busy" in failure.message.orEmpty(),
                "Expected the reported error to be propagated, but got '${failure.message}'"
            )
        }
    }

    @Test
    fun `test - the IDE rejects a debuggerState request without a cdpUrl`() {
        IdeaKotlinJsBrowserDebugSession.startForIde().use { ide ->
            assertEquals(
                HttpURLConnection.HTTP_BAD_REQUEST,
                statusCodeOf("GET", "${ide.connectionUrl}$DEBUGGER_STATE_PATH")
            )
        }
    }

    @Test
    fun `test - the IDE rejects a request with a malformed body`() {
        IdeaKotlinJsBrowserDebugSession.startForIde().use { ide ->
            assertEquals(
                HttpURLConnection.HTTP_INTERNAL_ERROR,
                statusCodeOf("POST", "${ide.connectionUrl}$DEBUGGABLE_BROWSER_READY_PATH", body = "not JSON")
            )
        }
    }

    private fun statusCodeOf(method: String, url: String, body: String? = null): Int {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            if (body != null) {
                connection.doOutput = true
                connection.outputStream.use { it.write(body.encodeToByteArray()) }
            }
            connection.responseCode.also { connection.errorStream?.close() }
        } finally {
            connection.disconnect()
        }
    }

    private data class RecordedRequest(
        val method: String,
        val path: String,
        /** the raw, percent-encoded query, i.e. exactly what was put on the wire */
        val rawQuery: String?,
        val body: String,
    )

    private class MockIde(private val httpServer: HttpServer) : AutoCloseable {
        val requests = mutableListOf<RecordedRequest>()
        val connectionUrl: String get() = "http://${httpServer.address.hostString}:${httpServer.address.port}"
        override fun close() = httpServer.stop(0)
    }

    /**
     * A minimal stand-in for the IDE side, used to observe exactly what the build system sends and to
     * simulate responses the real IDE side would never produce.
     */
    private fun withMockIde(
        statusCode: Int = HttpURLConnection.HTTP_OK,
        respond: (HttpExchange) -> String,
        action: (MockIde) -> Unit,
    ) {
        val httpServer = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        val mockIde = MockIde(httpServer)
        httpServer.createContext(BASE_PATH) { exchange ->
            try {
                mockIde.requests += RecordedRequest(
                    method = exchange.requestMethod,
                    path = exchange.requestURI.path,
                    rawQuery = exchange.requestURI.rawQuery,
                    body = exchange.requestBody.use { it.readBytes() }.decodeToString(),
                )
                val body = respond(exchange).encodeToByteArray()
                exchange.sendResponseHeaders(statusCode, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            } finally {
                exchange.close()
            }
        }
        httpServer.start()
        mockIde.use(action)
    }
}
