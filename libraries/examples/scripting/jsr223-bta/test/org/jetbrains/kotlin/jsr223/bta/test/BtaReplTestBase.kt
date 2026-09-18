/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jsr223.bta.test

import org.jetbrains.kotlin.jsr223.bta.deleteRecursivelyWithRetries
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries

abstract class BtaReplTestBase {

    private lateinit var daemonDir: Path

    private val toClose = mutableListOf<AutoCloseable>()

    protected val daemonRunFilesPath: Path get() = daemonDir.resolve("run")

    protected val daemonLogsPath: Path get() = daemonDir.resolve("logs")

    protected fun <T : AutoCloseable> closeAfterTest(closeable: T): T = closeable.also { toClose += it }

    @BeforeEach
    fun createDaemonDir() {
        daemonDir = Files.createTempDirectory("jsr223-bta-daemon-")
    }

    @AfterEach
    fun closeAndCleanUp() {
        for (closeable in toClose.asReversed()) {
            closeable.close()
        }
        toClose.clear()
        requestDaemonShutdown()
        daemonDir.deleteRecursivelyWithRetries(timeoutMillis = DAEMON_SHUTDOWN_TIMEOUT_MILLIS)
    }

    private fun requestDaemonShutdown() {
        if (!daemonRunFilesPath.isDirectory()) return
        for (runFile in daemonRunFilesPath.listDirectoryEntries("*.run")) {
            try {
                runFile.deleteIfExists()
            } catch (_: IOException) {
            }
        }
    }
}

private const val DAEMON_SHUTDOWN_TIMEOUT_MILLIS = 60_000L
