/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.debugger

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import kotlin.time.Duration

/**
 * A browser instance launched by the build system that a debugger can attach to.
 *
 * [cdpUrl] is the HTTP endpoint of the browser's Chrome DevTools Protocol, e.g. `http://127.0.0.1:52134`.
 * The debugger is expected to discover the actual targets and their WebSocket endpoints from it
 * (`$cdpUrl/json/version`, `$cdpUrl/json/list`).
 * More info: https://chromedevtools.github.io/devtools-protocol/
 */
@ExperimentalKotlinGradlePluginApi
class IdeaKotlinDebuggableBrowser(
    val cdpUrl: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IdeaKotlinDebuggableBrowser

        return cdpUrl == other.cdpUrl
    }

    override fun hashCode(): Int {
        return cdpUrl.hashCode()
    }
}

/**
 * Handshake between the IDE and the build system used to debug Kotlin/JS (WasmJS) tests running in a browser.
 *
 * The IDE hosts the http session ([IdeSession], and passes its [connectionUrl] to the build system,
 * which connects to it ([BuildSystemSession]) and drives the following
 * exchange:
 *
 * ```
 * build system                                              IDE
 *      |                                                     |
 *      |  POST /debuggableBrowserReady {"cdpUrl": "..."}     |  awaitBrowser() returns
 *      | --------------------------------------------------> |
 *      |  GET  /debuggerState?cdpUrl=...                     |  attaches the debugger, then
 *      | <--> {"state": "WAITING"} ... {"state": "READY"}    |  sendDebuggerReady(browser)
 *      |                                                     |
 *      |  (tests are executed in the browser)                |
 *      |                                                     |
 *      |  POST /finish {"cdpUrl": "..."}                     |  awaitFinished() returns
 *      | --------------------------------------------------> |
 * ```
 *
 * Either side may [abort] the session at any point: the other side then fails with [ConnectionAborted]
 * instead of waiting for a peer that will never respond.
 *
 * All messages are encoded as JSON, see `IdeaKotlinJsBrowserDebugSessionProtocol` for the wire format.
 */
@ExperimentalKotlinGradlePluginApi
sealed interface IdeaKotlinJsBrowserDebugSession {

    /**
     * Base URL of the session's HTTP server.
     *
     * The IDE passes this value to the build system; for Gradle builds it is the
     * `kotlin.internal.js.ideDebugSessionUrl` Gradle property.
     */
    val connectionUrl: String

    /**
     * Aborts the session, the counterpart session fails with [ConnectionAborted] on its next interaction.
     */
    fun abort(reason: String)

    /**
     * The IDE side of the session: hosts the HTTP server and attaches the debugger to the browser
     * reported by the build system.
     */
    @ExperimentalKotlinGradlePluginApi
    interface IdeSession : IdeaKotlinJsBrowserDebugSession, AutoCloseable {
        /**
         * Waits until the build system reports the browser it has launched.
         *
         * @throws ConnectionAborted if the session was aborted or [timeout] has elapsed.
         */
        fun awaitBrowser(timeout: Duration): IdeaKotlinDebuggableBrowser

        /**
         * Tells the build system that the debugger is attached to [browser] and that the tests may start.
         *
         * @throws IllegalArgumentException if [browser] is not the browser reported by the build system.
         */
        fun sendDebuggerReady(browser: IdeaKotlinDebuggableBrowser)

        /**
         * Waits until the build system reports that the test execution has finished.
         *
         * @throws ConnectionAborted if the session was aborted or [timeout] has elapsed.
         */
        fun awaitFinished(timeout: Duration)
    }

    /**
     * The build system side of the session: launches the browser and waits for the IDE to attach the
     * debugger to it before running the tests.
     */
    @ExperimentalKotlinGradlePluginApi
    interface BuildSystemSession : IdeaKotlinJsBrowserDebugSession {
        /**
         * Reports the launched [browser] to the IDE.
         *
         * @throws ConnectionAborted if the IDE rejected the browser or is not reachable.
         */
        fun sendBrowserReady(browser: IdeaKotlinDebuggableBrowser)

        /**
         * Waits until the IDE has attached its debugger to [forBrowser].
         *
         * @throws ConnectionAborted if the session was aborted or [timeout] has elapsed.
         */
        fun awaitDebuggerReady(forBrowser: IdeaKotlinDebuggableBrowser, timeout: Duration)

        /**
         * Reports that the tests running in [forBrowser] have finished and the browser is about to be closed.
         */
        fun sendFinished(forBrowser: IdeaKotlinDebuggableBrowser)
    }

    /**
     *
     */
    @ExperimentalKotlinGradlePluginApi
    class ConnectionAborted(
        message: String,
        cause: Throwable? = null
    ) : RuntimeException(message, cause)

    @ExperimentalKotlinGradlePluginApi
    companion object {
        /**
         * Starts a new session on a free port of the loopback interface.
         */
        fun startForIde(): IdeSession = IdeaKotlinJsBrowserDebugSessionServer().also { it.start() }

        /**
         * Connects to the session hosted by the IDE at [connectionUrl].
         */
        fun connectWithBuildSystem(connectionUrl: String): BuildSystemSession =
            IdeaKotlinJsBrowserDebugSessionClient(connectionUrl)
    }
}
