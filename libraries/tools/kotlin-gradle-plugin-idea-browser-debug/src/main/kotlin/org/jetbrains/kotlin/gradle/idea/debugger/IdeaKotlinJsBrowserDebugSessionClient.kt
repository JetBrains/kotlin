/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.debugger

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import org.jetbrains.kotlin.gradle.idea.debugger.IdeaKotlinJsBrowserDebugSession.ConnectionAborted
import org.jetbrains.kotlin.gradle.idea.debugger.IdeaKotlinJsBrowserDebugSessionProtocol.ABORT_PATH
import org.jetbrains.kotlin.gradle.idea.debugger.IdeaKotlinJsBrowserDebugSessionProtocol.CDP_URL_QUERY_PARAMETER
import org.jetbrains.kotlin.gradle.idea.debugger.IdeaKotlinJsBrowserDebugSessionProtocol.CONTENT_TYPE
import org.jetbrains.kotlin.gradle.idea.debugger.IdeaKotlinJsBrowserDebugSessionProtocol.DEBUGGABLE_BROWSER_READY_PATH
import org.jetbrains.kotlin.gradle.idea.debugger.IdeaKotlinJsBrowserDebugSessionProtocol.DEBUGGER_STATE_PATH
import org.jetbrains.kotlin.gradle.idea.debugger.IdeaKotlinJsBrowserDebugSessionProtocol.FINISH_PATH
import org.jetbrains.kotlin.gradle.idea.debugger.IdeaKotlinJsBrowserDebugSessionProtocol.json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_OK
import java.net.URL
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

internal class IdeaKotlinJsBrowserDebugSessionClient(
    override val connectionUrl: String,
    private val pollingInterval: Duration = 200.milliseconds,
    private val requestTimeout: Duration = 30.seconds,
) : IdeaKotlinJsBrowserDebugSession.BuildSystemSession {

    private val baseUrl = connectionUrl.trimEnd('/')

    override fun sendBrowserReady(browser: IdeaKotlinDebuggableBrowser) {
        postBrowser(DEBUGGABLE_BROWSER_READY_PATH, browser, "report the debuggable browser '${browser.cdpUrl}'")
    }

    @OptIn(ExperimentalTime::class)
    override fun awaitDebuggerReady(forBrowser: IdeaKotlinDebuggableBrowser, timeout: Duration) {
        val action = "wait for the debugger to attach to '${forBrowser.cdpUrl}'"
        val waiting = "attach its debugger to '${forBrowser.cdpUrl}'"
        val path = "$DEBUGGER_STATE_PATH?$CDP_URL_QUERY_PARAMETER=${encodeUrlComponent(forBrowser.cdpUrl)}"

        val startTime = TimeSource.Monotonic.markNow()

        while (true) {
            val response = request("GET", path, body = null, action = action)
            response.requireOk(action)
            val state = response.decode(IdeaKotlinDebuggerStateMessage.serializer(), action)
            when (state.state) {
                IdeaKotlinDebuggerState.DEBUGGER_READY -> return
                IdeaKotlinDebuggerState.ABORTED -> throw ConnectionAborted(
                    "Kotlin/JS browser debug session was aborted by the IDE: ${state.reason}"
                )
                IdeaKotlinDebuggerState.WAITING_FOR_DEBUGGER -> Unit
            }
            if (startTime.elapsedNow() >= timeout) {
                throw ConnectionAborted("Timed out after $timeout while waiting for the IDE to $waiting")
            }
            try {
                Thread.sleep(pollingInterval.inWholeMilliseconds)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw ConnectionAborted("Interrupted while waiting for the IDE to $waiting", e)
            }
        }
    }

    override fun sendFinished(forBrowser: IdeaKotlinDebuggableBrowser) {
        postBrowser(FINISH_PATH, forBrowser, "report that the tests in '${forBrowser.cdpUrl}' have finished")
    }

    override fun abort(reason: String) {
        val body = json.encodeToString(IdeaKotlinAbortSessionMessage.serializer(), IdeaKotlinAbortSessionMessage(reason))
        try {
            request("POST", ABORT_PATH, body, action = "abort the debug session")
        } catch (_: ConnectionAborted) { // ignore if server or someone else already aborted the session
        }
    }

    private fun postBrowser(path: String, browser: IdeaKotlinDebuggableBrowser, action: String) {
        val body = json.encodeToString(
            IdeaKotlinDebuggableBrowserMessage.serializer(),
            IdeaKotlinDebuggableBrowserMessage(browser.cdpUrl)
        )
        val response = request("POST", path, body, action)
        response.requireOk(action)
        response.decode(IdeaKotlinAcknowledgedMessage.serializer(), action)
    }

    private fun request(method: String, path: String, body: String?, action: String): Response {
        // Use of HttpUrlConnection is needed because Kotlin Gradle Plugin must be compatible with Java 8
        // HttpClient is available in Java 11+
        val connection = try {
            URL("$baseUrl$path").openConnection() as HttpURLConnection
        } catch (e: IOException) {
            throw ConnectionAborted("Failed to $action: cannot connect to the IDE at '$connectionUrl'", e)
        }

        try {
            connection.requestMethod = method
            connection.connectTimeout = requestTimeout.inWholeMilliseconds.toInt()
            connection.readTimeout = requestTimeout.inWholeMilliseconds.toInt()
            connection.setRequestProperty("Accept", CONTENT_TYPE)
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", CONTENT_TYPE)
                connection.outputStream.use { it.write(body.encodeToByteArray()) }
            }

            val statusCode = connection.responseCode
            val responseStream = if (statusCode < HTTP_BAD_REQUEST) connection.inputStream else connection.errorStream
            val responseBody = responseStream?.use { it.readBytes() }?.decodeToString().orEmpty()
            return Response(statusCode, responseBody)
        } catch (e: IOException) {
            throw ConnectionAborted("Failed to $action: the IDE at '$connectionUrl' is not reachable", e)
        } finally {
            connection.disconnect()
        }
    }

    private class Response(val statusCode: Int, val body: String)

    private fun Response.requireOk(action: String) {
        if (statusCode == HTTP_OK) return
        val error = try {
            json.decodeFromString(IdeaKotlinSessionErrorMessage.serializer(), body).error
        } catch (_: SerializationException) {
            body
        }
        throw ConnectionAborted("Failed to $action: the IDE responded with $statusCode: $error")
    }

    private fun <T> Response.decode(deserializer: DeserializationStrategy<T>, action: String): T = try {
        json.decodeFromString(deserializer, body)
    } catch (e: SerializationException) {
        throw ConnectionAborted("Failed to $action: unexpected response from the IDE: '$body'", e)
    }
}
