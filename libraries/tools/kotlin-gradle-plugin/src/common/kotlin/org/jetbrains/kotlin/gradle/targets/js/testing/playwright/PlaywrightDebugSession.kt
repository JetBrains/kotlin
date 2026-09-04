/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.playwright

import org.jetbrains.kotlin.gradle.idea.debugger.IdeaKotlinDebuggableBrowser
import org.jetbrains.kotlin.gradle.idea.debugger.IdeaKotlinJsBrowserDebugSession
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readLines

private val log = LoggerFactory.getLogger(PlaywrightDebugSession::class.java)

internal class PlaywrightDebugSession(
    private val session: IdeaKotlinJsBrowserDebugSession.BuildSystemSession,
) {
    private var browser: IdeaKotlinDebuggableBrowser? = null

    fun launchArgs(runner: PwRunnerSpec): List<String> =
        // users can change this in their launchArgs
        listOf("--remote-debugging-port=0") + runner.launchArgs

    fun attachDebugger(runner: PwRunnerSpec) {
        val browser = IdeaKotlinDebuggableBrowser(cdpUrl = readCdpUrl(runner.browserDataDir))
        this.browser = browser
        log.info("Reporting debuggable browser '{}' of runner '{}' to the IDE", browser.cdpUrl, runner.name)
        session.sendBrowserReady(browser)
        log.info("Waiting for the IDE to attach its debugger to '{}'", browser.cdpUrl)
        session.awaitDebuggerReady(browser, timeout = runner.timeout)
    }

    fun reportFinished() {
        val browser = browser ?: return
        session.sendFinished(browser)
    }

    fun abort(reason: String) {
        session.abort(reason)
    }

    private fun readCdpUrl(userDataDir: Path): String {
        val portFile = userDataDir.resolve(DEV_TOOLS_ACTIVE_PORT_FILE)

        check(portFile.exists()) {
            "Failed to read the Chrome DevTools Protocol port of the browser from '$portFile'."
        }

        val port = portFile.readLines().firstOrNull()?.trim()?.toIntOrNull()
        check(port != null && port > 0) {
            "Failed to read the Chrome DevTools Protocol port of the browser from '$portFile'. Invalid file content."
        }

        return "http://${InetAddress.getLoopbackAddress().hostAddress}:$port"
    }

    companion object {
        private const val DEV_TOOLS_ACTIVE_PORT_FILE = "DevToolsActivePort"

        fun connect(connectionUrl: String): PlaywrightDebugSession =
            PlaywrightDebugSession(IdeaKotlinJsBrowserDebugSession.connectWithBuildSystem(connectionUrl))
    }
}
