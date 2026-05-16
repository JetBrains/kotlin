/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223.daemon.test

import kotlin.script.experimental.jvmhost.jsr223.daemon.DaemonReplCompiler
import org.jetbrains.kotlin.daemon.common.DaemonLogOptions
import org.jetbrains.kotlin.daemon.common.DaemonOptions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.util.LinkedSnippet

/**
 * Exercises [DaemonReplCompiler] directly (bypassing the
 * [kotlin.script.experimental.jvmhost.jsr223.daemon.KotlinJsr223DaemonScriptEngineImpl] layer) to
 * verify its message-collector handling: a snippet's (non-error) messages -- e.g. warnings -- must
 * be surfaced on a *successful* compile too, and must never leak into a *later* snippet's report.
 */
class DaemonReplCompilerTest {

    @TempDir
    lateinit var daemonRunDir: Path

    private val compilerClasspath: List<File> = classpathFromSystemProperty("kotlinJsr223DaemonCompilerClasspath")

    // See KotlinJsr223DaemonScriptEngineTest's identical property for why this, rather than a
    // dedicated implementation classloader, is enough here.
    private val stdlib: File by lazy {
        File(KotlinVersion::class.java.protectionDomain.codeSource.location.toURI())
    }

    // See KotlinJsr223DaemonScriptEngineTest's tearDown for why this is needed now that the daemon
    // connection is leased once and cached for the compiler's whole lifetime.
    private val compilersToShutDown = mutableListOf<DaemonReplCompiler>()

    private fun newCompiler(): DaemonReplCompiler =
        DaemonReplCompiler(
            compilerClasspath = compilerClasspath,
            additionalClasspath = listOf(stdlib.toPath()),
            daemonOptions = DaemonOptions(
                runFilesPath = daemonRunDir.resolve("run").toString(),
                shutdownDelayMilliseconds = 0,
            ),
            daemonLogOptions = DaemonLogOptions(logsPath = daemonRunDir.resolve("logs").toString()),
        ).also { compilersToShutDown += it }

    @AfterEach
    fun tearDown() {
        for (compiler in compilersToShutDown) {
            compiler.forceShutdownDaemon()
        }
        compilersToShutDown.clear()
    }

    @Suppress("DEPRECATION_ERROR")
    private fun compile(
        compiler: DaemonReplCompiler,
        source: String,
        name: String,
    ): ResultWithDiagnostics<LinkedSnippet<*>> =
        internalScriptingRunSuspend { compiler.compile(source.toScriptSource(name), ScriptCompilationConfiguration()) }

    // The daemon always reports a handful of fixed, snippet-independent warnings on every compile
    // (a deprecated-flag notice, plus "jar not found in the Kotlin home directory" notices --
    // this classpath never has a "Kotlin home" of its own). Before this fix, DaemonReplCompiler
    // collected these but never attached them to a *successful* result's reports -- only a
    // Failure carried any diagnostics at all.
    @Test
    fun testMessagesAreReportedOnSuccessfulCompile() {
        val compiler = newCompiler()
        val result = compile(compiler, "1", "snippet_0.repl.kts")
        val success = result as? ResultWithDiagnostics.Success ?: error("expected a successful compile, got: $result")
        assertTrue(
            success.reports.isNotEmpty() && success.reports.all { it.severity == ScriptDiagnostic.Severity.WARNING },
            "expected the daemon's fixed warnings among the successful compile's reports, got: ${success.reports}"
        )
    }

    // If messageCollector (now a compiler-level field, shared by every compile) were not reset
    // before each compile, this second, otherwise-identical successful compile would end up
    // reporting both compiles' fixed warnings together (8 messages instead of 4).
    @Test
    fun testMessagesDoNotAccumulateAcrossCompilations() {
        val compiler = newCompiler()
        val firstResult = compile(compiler, "1", "snippet_0.repl.kts") as? ResultWithDiagnostics.Success
            ?: error("expected the first snippet to compile successfully")
        val secondResult = compile(compiler, "2", "snippet_1.repl.kts") as? ResultWithDiagnostics.Success
            ?: error("expected the second snippet to compile successfully")
        assertTrue(firstResult.reports.isNotEmpty(), "expected the first compile to report the daemon's fixed warnings")
        assertEquals(
            firstResult.reports.size, secondResult.reports.size,
            "the second compile's reports must not include the first compile's stale messages on top of its own;" +
                    " first=${firstResult.reports}, second=${secondResult.reports}"
        )
    }
}

private fun classpathFromSystemProperty(propertyName: String): List<File> =
    System.getProperty(propertyName)
        ?.split(File.pathSeparator)
        ?.filter { it.isNotBlank() }
        ?.map { File(it) }
        ?: error("system property '$propertyName' is not set -- run this test via its Gradle test task")
