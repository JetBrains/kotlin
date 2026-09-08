/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.debugger

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import org.jetbrains.kotlin.gradle.idea.debugger.IdeaKotlinJsBrowserDebugSessionProtocol.CONTENT_TYPE
import org.jetbrains.kotlin.gradle.idea.debugger.IdeaKotlinJsBrowserDebugSessionProtocol.json
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_OK
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

internal class IdeaKotlinJsBrowserDebugSessionServer(
    private val httpServer: HttpServer = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0),
) : IdeaKotlinJsBrowserDebugSession.IdeSession {

    private val lock = ReentrantLock()
    private val stateChanged = lock.newCondition()

    private var browser: IdeaKotlinDebuggableBrowser? = null
    private var debuggerReady: Boolean = false
    private var finished: Boolean = false
    private var abortReason: String? = null
    private var httpServerStarted: Boolean = false

    /**
     * Code that updates and reads the state from variables declared above (i.e. [debuggerReady])
     * should be wrapped with [withStateLock], and return `true` if the state changed.
     *
     * This is necessary to avoid race conditions.
     */
    private inline fun withStateLock(action: () -> Boolean) {
        lock.withLock {
            if (action()) stateChanged.signalAll()
        }
    }

    /** Same as [withStateLock] */
    private inline fun <T> withStateLockReturn(action: () -> Pair<Boolean, T>) {
        lock.withLock {
            val (changed, result) = action()
            if (changed) stateChanged.signalAll()
            result
        }
    }

    init {
        httpServer.createContext(IdeaKotlinJsBrowserDebugSessionProtocol.DEBUGGABLE_BROWSER_READY_PATH, handler("POST", ::handleDebuggableBrowserReady))
        httpServer.createContext(IdeaKotlinJsBrowserDebugSessionProtocol.DEBUGGER_STATE_PATH, handler("GET", ::handleDebuggerState))
        httpServer.createContext(IdeaKotlinJsBrowserDebugSessionProtocol.FINISH_PATH, handler("POST", ::handleFinish))
        httpServer.createContext(IdeaKotlinJsBrowserDebugSessionProtocol.ABORT_PATH, handler("POST", ::handleAbort))
    }

    fun start() {
        withStateLock {
            check(!httpServerStarted) { "Http server is already started" }
            httpServer.start()
            httpServerStarted = true
            true
        }
    }

    override val connectionUrl: String
        get() = "http://${httpServer.address.hostString}:${httpServer.address.port}"

    override fun awaitBrowser(timeout: Duration): IdeaKotlinDebuggableBrowser =
        await(timeout, what = "the build system to launch a debuggable browser") { browser }

    override fun sendDebuggerReady(browser: IdeaKotlinDebuggableBrowser) {
        withStateLock {
            check(httpServerStarted) { "Http server not started" }
            require(browser == this.browser) {
                "Unexpected browser '${browser.cdpUrl}', the build system reported '${this.browser?.cdpUrl}'"
            }
            debuggerReady = true
            true
        }
    }

    override fun awaitFinished(timeout: Duration) {
        await(timeout, what = "the build system to finish the test execution") { if (finished) Unit else null }
    }

    override fun abort(reason: String) {
        withStateLock {
            if (abortReason == null) {
                abortReason = reason
                true
            } else {
                false
            }
        }
    }

    override fun close() {
        withStateLock {
            if (abortReason == null && !finished) {
                abortReason = "the IDE closed the debug session"
                true
            } else {
                false
            }
        }
        httpServer.stop(0)
    }

    /**
     * Blocks until [state] is available, i.e. until an HTTP handler published the awaited state change.
     */
    private fun <T : Any> await(timeout: Duration, what: String, state: () -> T?): T {
        lock.withLock {
            check(httpServerStarted) { "Http server not started" }
            var remainingNanos = timeout.inWholeNanoseconds
            while (true) {
                abortReason?.let { reason ->
                    throw IdeaKotlinJsBrowserDebugSession.ConnectionAborted("Kotlin/JS browser debug session was aborted: $reason")
                }
                state()?.let { return it }
                if (remainingNanos <= 0) {
                    throw IdeaKotlinJsBrowserDebugSession.ConnectionAborted("Timed out after $timeout while waiting for $what")
                }
                remainingNanos = stateChanged.awaitNanos(remainingNanos)
            }
        }
    }

    private fun handleDebuggableBrowserReady(exchange: HttpExchange) {
        val reportedBrowser = IdeaKotlinDebuggableBrowser(
            exchange.receive(IdeaKotlinDebuggableBrowserMessage.serializer()).cdpUrl
        )

        withStateLock {
            // check if aborted previously
            if (abortReason != null) {
                exchange.respondError(
                    HttpURLConnection.HTTP_CONFLICT,
                    "The debug session was aborted: $abortReason"
                )
                return@withStateLock false
            }

            // check browser was already reported, current implementation doesn't support multiple browsers
            // allow idempotent calls
            if (browser != null && browser != reportedBrowser) {
                exchange.respondError(
                    HttpURLConnection.HTTP_CONFLICT,
                    "A debuggable browser was already reported for this session: '${browser!!.cdpUrl}'"
                )
                return@withStateLock false
            }

            exchange.respondAcknowledged()

            if (browser == null) {
                browser = reportedBrowser
                true
            } else {
                false
            }
        }
    }

    private fun handleDebuggerState(exchange: HttpExchange) {
        val cdpUrl = exchange.queryParameter(IdeaKotlinJsBrowserDebugSessionProtocol.CDP_URL_QUERY_PARAMETER)
        if (cdpUrl == null) {
            exchange.respondError(HttpURLConnection.HTTP_BAD_REQUEST, "Missing '${IdeaKotlinJsBrowserDebugSessionProtocol.CDP_URL_QUERY_PARAMETER}' query parameter")
            return
        }

        withStateLock {
            // check if it is known browser
            if (browser == null || browser!!.cdpUrl != cdpUrl) {
                exchange.respondError(
                    HttpURLConnection.HTTP_NOT_FOUND,
                    "Unknown debuggable browser '$cdpUrl'"
                )
                return@withStateLock false
            }

            val message = when {
                abortReason != null -> IdeaKotlinDebuggerStateMessage(IdeaKotlinDebuggerState.ABORTED, abortReason)
                debuggerReady -> IdeaKotlinDebuggerStateMessage(IdeaKotlinDebuggerState.DEBUGGER_READY)
                else -> IdeaKotlinDebuggerStateMessage(IdeaKotlinDebuggerState.WAITING_FOR_DEBUGGER)
            }

            exchange.respond(
                HttpURLConnection.HTTP_OK,
                IdeaKotlinDebuggerStateMessage.serializer(),
                message
            )

            false
        }
    }

    private fun handleFinish(exchange: HttpExchange) {
        val cdpUrl = exchange.receive(IdeaKotlinDebuggableBrowserMessage.serializer()).cdpUrl

        withStateLock {
            // check if aborted previously
            if (abortReason != null) {
                exchange.respondError(
                    HttpURLConnection.HTTP_CONFLICT,
                    "The debug session was aborted: $abortReason"
                )
                return@withStateLock false
            }

            // check if it is known browser
            if (browser == null || browser!!.cdpUrl != cdpUrl) {
                exchange.respondError(
                    HttpURLConnection.HTTP_NOT_FOUND,
                    "Unknown debuggable browser '$cdpUrl'"
                )
                return@withStateLock false
            }

            exchange.respondAcknowledged()
            if (!finished) {
                finished = true
                true
            } else {
                false
            }
        }
    }

    private fun handleAbort(exchange: HttpExchange) {
        val reason = exchange.receive(IdeaKotlinAbortSessionMessage.serializer()).reason
        abort("reported by the build system: $reason")
        exchange.respondAcknowledged()
    }

    private fun handler(method: String, handle: (HttpExchange) -> Unit): HttpHandler = HttpHandler { exchange ->
        try {
            if (exchange.requestMethod != method) {
                exchange.respondError(HttpURLConnection.HTTP_BAD_METHOD, "Expected a $method request, but got ${exchange.requestMethod}")
            } else {
                handle(exchange)
            }
        } catch (throwable: Throwable) {
            // The response may have been sent already, in which case there is nothing left to report.
            try {
                exchange.respondError(HttpURLConnection.HTTP_INTERNAL_ERROR, throwable.toString())
            } catch (_: Throwable) {
            }
        } finally {
            exchange.close()
        }
    }
}

private fun HttpExchange.queryParameter(name: String): String? = requestURI.rawQuery
    ?.split("&")
    ?.firstOrNull { it.startsWith("$name=") }
    ?.substringAfter("=")
    ?.let { decodeUrlComponent(it) }

private fun <T> HttpExchange.receive(deserializer: DeserializationStrategy<T>): T =
    json.decodeFromString(deserializer, requestBody.use { it.readBytes() }.decodeToString())

private fun HttpExchange.respondAcknowledged() =
    respond(HTTP_OK, IdeaKotlinAcknowledgedMessage.serializer(), IdeaKotlinAcknowledgedMessage())

private fun HttpExchange.respondError(statusCode: Int, error: String) =
    respond(statusCode, IdeaKotlinSessionErrorMessage.serializer(), IdeaKotlinSessionErrorMessage(error))

private fun <T> HttpExchange.respond(statusCode: Int, serializer: SerializationStrategy<T>, message: T) {
    val body = json.encodeToString(serializer, message).encodeToByteArray()
    responseHeaders.add("Content-Type", CONTENT_TYPE)
    sendResponseHeaders(statusCode, body.size.toLong())
    responseBody.use { it.write(body) }
}
