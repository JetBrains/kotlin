/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.daemon.client.DaemonReportingTargets
import org.jetbrains.kotlin.daemon.client.KotlinCompilerClient
import org.jetbrains.kotlin.daemon.common.COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX
import org.jetbrains.kotlin.daemon.common.COMPILE_DAEMON_IS_READY_MESSAGE
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerId
import org.jetbrains.kotlin.daemon.common.DaemonJVMOptions
import org.jetbrains.kotlin.daemon.common.DaemonOptions
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds

abstract class DaemonSession(javaHome: String = System.getProperty("java.home")) : AutoCloseable {
    protected abstract val compilerId: CompilerId
    protected abstract val executable: String

    private val workingDir = Files.createTempDirectory("kotlin-daemon-").toFile()
    private val outputFile = workingDir.resolve("daemon.out").apply { createNewFile() }
    private val logFile = workingDir.resolve("daemon.log").apply { createNewFile() }
    private val clientAliveFile = workingDir.resolve("client.alive").apply {
        createNewFile()
        deleteOnExit()
    }

    protected open val jvmArgs: List<String> by lazy {
        val kotlinHome = ForTestCompileRuntime.distKotlincForTests()
        listOf(
            "-Djava.home=$javaHome",
            "-Dkotlin.home=${kotlinHome.absolutePath}",
            "-Djava.awt.headless=true",
            "-Djava.rmi.server.hostname=127.0.0.1",
            "-D${CompilerSystemProperties.COMPILE_DAEMON_LOG_PATH_PROPERTY.property}=${logFile.absolutePath}",
            "-D${CompilerSystemProperties.COMPILE_DAEMON_INITIATOR_MARKER_FILE.property}=${clientAliveFile.absolutePath}",
        ) + SubprocessCompilerRunner.DEFAULT_JVM_ARGS
    }

    private val daemonIsReady = AtomicBoolean(false)
    private val startupFinished = CountDownLatch(1)

    private lateinit var process: Process
    private lateinit var outputReader: Thread

    private val compileServiceDelegate = lazy {
        val daemonOptions = DaemonOptions(runFilesPath = workingDir.resolve("daemon-registry").absolutePath)
        val command = listOf(executable) + jvmArgs +
                daemonOptions.mappers.flatMap { it.toArgs(COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX) } +
                compilerId.mappers.flatMap { it.toArgs(COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX) }
        process = ProcessBuilder(command)
            .directory(workingDir)
            .redirectErrorStream(true)
            .start()

        outputReader = thread(isDaemon = true, name = "kotlin-daemon-output") {
            try {
                process.inputStream.bufferedReader().forEachLine { line ->
                    outputFile.appendText(line + "\n")
                    if (COMPILE_DAEMON_IS_READY_MESSAGE in line) {
                        daemonIsReady.set(true)
                        startupFinished.countDown()
                    }
                }
            } finally {
                startupFinished.countDown()
            }
        }

        try {
            awaitDaemonReady()
            KotlinCompilerClient.connectAndLease(
                compilerId,
                clientAliveFile,
                DaemonJVMOptions(reservedCodeCacheSize = ""),
                daemonOptions,
                DaemonReportingTargets(out = System.err),
                autostart = false,
                leaseSession = false,
            )?.compileService ?: error("Could not connect to compiler daemon")
        } catch (e: Throwable) {
            val logs = daemonOutput()
            cleanup()
            throw IllegalStateException("Compiler daemon startup failed\n$logs", e)
        }
    }

    val compileService: CompileService by compileServiceDelegate

    private fun awaitDaemonReady() {
        check(startupFinished.await(STARTUP_WAIT_TIMEOUT.inWholeSeconds, TimeUnit.SECONDS)) {
            "Timed out starting compiler daemon"
        }
        check(daemonIsReady.get()) {
            if (process.isAlive) "Compiler daemon closed its output before becoming ready"
            else "Compiler daemon exited with code ${process.exitValue()}"
        }
    }

    private fun daemonOutput(): String = buildString {
        appendLine("Stdout:")
        appendLine(outputFile.readText())
        appendLine("Stderr:")
        appendLine(logFile.readText())
    }

    override fun close() {
        if (!compileServiceDelegate.isInitialized()) {
            workingDir.deleteRecursively()
            return
        }

        val shutdown = thread(isDaemon = true, name = "kotlin-daemon-shutdown") {
            try {
                compileService.shutdown()
            } catch (e: Throwable) {
                System.err.println("Compiler daemon shutdown failed: $e\n${daemonOutput()}")
            }
        }
        shutdown.join(SHUTDOWN_WAIT_TIMEOUT.inWholeMilliseconds)
        if (shutdown.isAlive) {
            System.err.println("Compiler daemon shutdown timed out\n${daemonOutput()}")
        }
        cleanup()
    }

    private fun cleanup() {
        if (!process.waitFor(SHUTDOWN_WAIT_TIMEOUT.inWholeMilliseconds, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
        }
        outputReader.join(SHUTDOWN_WAIT_TIMEOUT.inWholeMilliseconds)
        workingDir.deleteRecursively()
    }

    companion object {
        private val STARTUP_WAIT_TIMEOUT = 30.seconds
        private val SHUTDOWN_WAIT_TIMEOUT = 5.seconds
    }
}
