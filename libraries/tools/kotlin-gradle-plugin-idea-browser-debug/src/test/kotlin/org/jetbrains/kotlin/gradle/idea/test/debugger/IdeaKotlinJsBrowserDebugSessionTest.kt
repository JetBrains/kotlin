/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.test.debugger

import org.jetbrains.kotlin.gradle.idea.debugger.*
import org.jetbrains.kotlin.gradle.idea.debugger.IdeaKotlinJsBrowserDebugSession.ConnectionAborted
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Tests the [IdeaKotlinJsBrowserDebugSession] handshake by running both of its sides against each other:
 * a real IDE side ([IdeaKotlinJsBrowserDebugSession.IdeSession], an HTTP server on the loopback interface)
 * and a real build system side ([IdeaKotlinJsBrowserDebugSession.BuildSystemSession], an HTTP client).
 */
class IdeaKotlinJsBrowserDebugSessionTest {

    private val browser = IdeaKotlinDebuggableBrowser(cdpUrl = "http://127.0.0.1:9222")

    @Test
    fun `test - full handshake`() {
        withIdeSession { ide ->
            val buildSystem = connect(ide)

            val build = submit {
                buildSystem.sendBrowserReady(browser)
                buildSystem.awaitDebuggerReady(browser, timeout = 10.seconds)
                // 'awaitDebuggerReady' only returns once the debugger is attached
                buildSystem.sendFinished(browser)
            }

            assertEquals(browser, ide.awaitBrowser(timeout = 10.seconds))
            ide.sendDebuggerReady(browser)
            ide.awaitFinished(timeout = 10.seconds)

            build.await()
        }
    }

    @Test
    fun `test - awaitBrowser times out when the build system never connects`() {
        withIdeSession { ide ->
            val failure = assertFailsWith<ConnectionAborted> { ide.awaitBrowser(timeout = 100.milliseconds) }
            assertTrue(
                "Timed out" in failure.message.orEmpty(),
                "Expected a timeout failure, but got '${failure.message}'"
            )
        }
    }

    @Test
    fun `test - awaitDebuggerReady times out when the IDE never attaches`() {
        withIdeSession { ide ->
            val buildSystem = connect(ide)
            buildSystem.sendBrowserReady(browser)
            assertEquals(browser, ide.awaitBrowser(timeout = 10.seconds))

            val failure = assertFailsWith<ConnectionAborted> {
                buildSystem.awaitDebuggerReady(browser, timeout = 300.milliseconds)
            }
            assertTrue(
                "Timed out" in failure.message.orEmpty(),
                "Expected a timeout failure, but got '${failure.message}'"
            )
        }
    }

    @Test
    fun `test - session aborted by the IDE - build system stops waiting`() {
        withIdeSession { ide ->
            val buildSystem = connect(ide)
            buildSystem.sendBrowserReady(browser)
            ide.awaitBrowser(timeout = 10.seconds)

            val build = submit {
                assertFailsWith<ConnectionAborted> { buildSystem.awaitDebuggerReady(browser, timeout = 10.seconds) }
            }

            ide.abort("the user stopped the debug session")

            val failure = build.await()
            assertTrue(
                "the user stopped the debug session" in failure.message.orEmpty(),
                "Expected the abort reason to be reported to the build system, but got '${failure.message}'"
            )
        }
    }

    @Test
    fun `test - session aborted by the build system - IDE stops waiting`() {
        withIdeSession { ide ->
            val buildSystem = connect(ide)

            val build = submit { buildSystem.abort("the browser could not be launched") }

            val failure = assertFailsWith<ConnectionAborted> { ide.awaitBrowser(timeout = 10.seconds) }
            assertTrue(
                "the browser could not be launched" in failure.message.orEmpty(),
                "Expected the abort reason to be reported to the IDE, but got '${failure.message}'"
            )

            build.await()
        }
    }

    @Test
    fun `test - session aborted by the build system - awaitFinished stops waiting`() {
        withIdeSession { ide ->
            val buildSystem = connect(ide)
            buildSystem.sendBrowserReady(browser)
            ide.awaitBrowser(timeout = 10.seconds)
            ide.sendDebuggerReady(browser)
            buildSystem.awaitDebuggerReady(browser, timeout = 10.seconds)

            val build = submit { buildSystem.abort("the tests crashed") }

            val failure = assertFailsWith<ConnectionAborted> { ide.awaitFinished(timeout = 10.seconds) }
            assertTrue(
                "the tests crashed" in failure.message.orEmpty(),
                "Expected the abort reason to be reported to the IDE, but got '${failure.message}'"
            )

            build.await()
        }
    }

    @Test
    fun `test - reporting the same browser twice is accepted`() {
        withIdeSession { ide ->
            val buildSystem = connect(ide)
            buildSystem.sendBrowserReady(browser)
            buildSystem.sendBrowserReady(browser)
            assertEquals(browser, ide.awaitBrowser(timeout = 10.seconds))
        }
    }

    @Test
    fun `test - reporting a second browser is rejected`() {
        withIdeSession { ide ->
            val buildSystem = connect(ide)
            buildSystem.sendBrowserReady(browser)

            val failure = assertFailsWith<ConnectionAborted> {
                buildSystem.sendBrowserReady(IdeaKotlinDebuggableBrowser(cdpUrl = "http://127.0.0.1:9333"))
            }
            assertTrue(
                browser.cdpUrl in failure.message.orEmpty(),
                "Expected the already reported browser to be named, but got '${failure.message}'"
            )

            // the session keeps working with the browser that was reported first
            assertEquals(browser, ide.awaitBrowser(timeout = 10.seconds))
        }
    }

    @Test
    fun `test - awaiting the debugger for an unreported browser is rejected`() {
        withIdeSession { ide ->
            val buildSystem = connect(ide)
            buildSystem.sendBrowserReady(browser)
            ide.awaitBrowser(timeout = 10.seconds)

            val unknown = IdeaKotlinDebuggableBrowser(cdpUrl = "http://127.0.0.1:9333")
            val failure = assertFailsWith<ConnectionAborted> {
                buildSystem.awaitDebuggerReady(unknown, timeout = 10.seconds)
            }
            assertTrue(
                unknown.cdpUrl in failure.message.orEmpty(),
                "Expected the unknown browser to be named, but got '${failure.message}'"
            )
        }
    }

    @Test
    fun `test - reporting finished for an unreported browser is rejected`() {
        withIdeSession { ide ->
            val buildSystem = connect(ide)
            buildSystem.sendBrowserReady(browser)
            ide.awaitBrowser(timeout = 10.seconds)

            assertFailsWith<ConnectionAborted> {
                buildSystem.sendFinished(IdeaKotlinDebuggableBrowser(cdpUrl = "http://127.0.0.1:9333"))
            }
        }
    }

    @Test
    fun `test - sendDebuggerReady for an unreported browser fails`() {
        withIdeSession { ide ->
            val buildSystem = connect(ide)
            buildSystem.sendBrowserReady(browser)
            ide.awaitBrowser(timeout = 10.seconds)

            assertFailsWith<IllegalArgumentException> {
                ide.sendDebuggerReady(IdeaKotlinDebuggableBrowser(cdpUrl = "http://127.0.0.1:9333"))
            }
        }
    }

    @Test
    fun `test - closing the session releases the IDE side`() {
        val ide = IdeaKotlinJsBrowserDebugSession.startForIde()
        val buildSystem = connect(ide)

        val ideSide = submit { assertFailsWith<ConnectionAborted> { ide.awaitBrowser(timeout = 10.seconds) } }
        ide.close()
        ideSide.await()

        // the server is gone, the build system reports it instead of hanging
        assertFailsWith<ConnectionAborted> { buildSystem.sendBrowserReady(browser) }
    }

    @Test
    fun `test - unreachable IDE is reported as an aborted connection`() {
        // port 0 is never listening
        val buildSystem = IdeaKotlinJsBrowserDebugSession.connectWithBuildSystem("http://127.0.0.1:1")
        assertFailsWith<ConnectionAborted> { buildSystem.sendBrowserReady(browser) }
        assertFailsWith<ConnectionAborted> { buildSystem.awaitDebuggerReady(browser, timeout = 1.seconds) }
        assertFailsWith<ConnectionAborted> { buildSystem.sendFinished(browser) }
        // aborting is best effort and must not throw, even when nobody is listening
        buildSystem.abort("no reason")
    }

    @Test
    fun `test - trailing slash in the connection url is accepted`() {
        withIdeSession { ide ->
            val buildSystem = IdeaKotlinJsBrowserDebugSession.connectWithBuildSystem(ide.connectionUrl + "/")
            buildSystem.sendBrowserReady(browser)
            assertEquals(browser, ide.awaitBrowser(timeout = 10.seconds))
        }
    }

    private fun withIdeSession(action: (IdeaKotlinJsBrowserDebugSession.IdeSession) -> Unit) {
        IdeaKotlinJsBrowserDebugSession.startForIde().use(action)
    }

    private fun connect(ide: IdeaKotlinJsBrowserDebugSession.IdeSession) =
        IdeaKotlinJsBrowserDebugSessionClient(ide.connectionUrl, pollingInterval = 20.milliseconds)

    /**
     * Runs [action] on another thread: every test here needs both sides of the handshake to make progress.
     */
    private fun <T> submit(action: () -> T): Awaitable<T> {
        val executor = Executors.newSingleThreadExecutor()
        return try {
            Awaitable(executor.submit(action), executor)
        } catch (throwable: Throwable) {
            executor.shutdownNow()
            throw throwable
        }
    }

    private class Awaitable<T>(
        private val future: java.util.concurrent.Future<T>,
        private val executor: java.util.concurrent.ExecutorService,
    ) {
        fun await(): T = try {
            try {
                future.get(30, TimeUnit.SECONDS)
            } catch (e: TimeoutException) {
                fail("The other side of the debug session did not finish in time", e)
            } catch (e: java.util.concurrent.ExecutionException) {
                throw e.cause ?: e
            }
        } finally {
            executor.shutdownNow()
        }
    }
}
