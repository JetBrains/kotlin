/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223.bta.test

import kotlin.script.experimental.jvmhost.jsr223.bta.BtaReplCompiler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.impl.internalScriptingRunSuspend
import kotlin.script.experimental.util.LinkedSnippet

/**
 * Exercises [BtaReplCompiler] directly, bypassing the script engine layer, to verify its
 * message-collector handling.
 */
class BtaReplCompilerTest {

    @TempDir
    lateinit var daemonRunDir: Path

    private val compilersToClose = mutableListOf<BtaReplCompiler>()

    private fun newCompiler(): BtaReplCompiler =
        BtaReplCompiler(
            compilerClasspath = classpathFromSystemProperty("kotlinJsr223BtaImplClasspath"),
            scriptingPluginClasspath = classpathFromSystemProperty("kotlinJsr223BtaScriptingPluginClasspath"),
            additionalClasspath = listOf(stdlibPath),
            daemonRunFilesPath = daemonRunDir.resolve("run"),
            daemonLogsPath = daemonRunDir.resolve("logs"),
            daemonShutdownDelayMillis = 0,
        ).also { compilersToClose += it }

    @AfterEach
    fun tearDown() {
        for (compiler in compilersToClose) {
            compiler.close()
        }
        compilersToClose.clear()
    }

    @Suppress("DEPRECATION_ERROR")
    private fun compile(
        compiler: BtaReplCompiler,
        source: String,
        name: String,
    ): ResultWithDiagnostics<LinkedSnippet<*>> =
        internalScriptingRunSuspend { compiler.compile(source.toScriptSource(name), ScriptCompilationConfiguration()) }

    // An unchecked cast compiles successfully but warns.
    @Test
    fun testMessagesAreReportedOnSuccessfulCompile() {
        val compiler = newCompiler()
        val result = compile(compiler, UNCHECKED_CAST_SNIPPET, "snippet_0.repl.kts")
        val success = result as? ResultWithDiagnostics.Success ?: error("expected a successful compile, got: $result")
        assertTrue(
            success.reports.any { it.severity == ScriptDiagnostic.Severity.WARNING && "Unchecked cast" in it.message },
            "expected the snippet's unchecked-cast warning among the successful compile's reports, got: ${success.reports}"
        )
    }

    @Test
    fun testMessagesDoNotAccumulateAcrossCompilations() {
        val compiler = newCompiler()
        compile(compiler, UNCHECKED_CAST_SNIPPET, "snippet_0.repl.kts") as? ResultWithDiagnostics.Success
            ?: error("expected the first snippet to compile successfully")
        val secondResult = compile(compiler, "2", "snippet_1.repl.kts") as? ResultWithDiagnostics.Success
            ?: error("expected the second snippet to compile successfully")
        assertFalse(
            secondResult.reports.any { "Unchecked cast" in it.message },
            "the second compile's reports must not include the first compile's stale messages, got: ${secondResult.reports}"
        )
    }
}

private const val UNCHECKED_CAST_SNIPPET = "val strings = listOf(1) as List<String>"
